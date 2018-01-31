package brs;

import brs.db.BurstKey.LongKeyFactory;
import brs.db.EntityTable;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.peer.Peer;
import brs.peer.Peers;
import brs.services.PropertyService;
import brs.util.JSON;
import brs.util.Listener;
import brs.util.Listeners;
import brs.util.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionProcessorImpl implements TransactionProcessor {

  private static final Logger logger = LoggerFactory.getLogger(TransactionProcessorImpl.class);

  private final boolean enableTransactionRebroadcasting;
  private final boolean testUnconfirmedTransactions;

  private final int rebroadcastAfter;
  private final int rebroadcastEvery;

  private final BurstKey.LongKeyFactory<TransactionImpl> unconfirmedTransactionDbKeyFactory;

  private final EntityTable<TransactionImpl> unconfirmedTransactionTable;

  private final Set<TransactionImpl> nonBroadcastedTransactions = Collections.newSetFromMap(new ConcurrentHashMap<TransactionImpl,Boolean>());
  private final Listeners<List<? extends Transaction>,Event> transactionListeners = new Listeners<>();
  private final Set<TransactionImpl> lostTransactions = new HashSet<>();
  private final Map<Long, Integer> lostTransactionHeights = new HashMap<>();

  private final Runnable removeUnconfirmedTransactionsThread = () -> {

    try {
      try {
        List<TransactionImpl> expiredTransactions = new ArrayList<>();
        try (BurstIterator<TransactionImpl> iterator = Burst.getStores().getTransactionProcessorStore().getExpiredTransactions()) {
          while (iterator.hasNext()) {
            expiredTransactions.add(iterator.next());
          }
        }
        if (expiredTransactions.size() > 0) {
          synchronized (this.blockchain) {
            try {
              Burst.getStores().beginTransaction();

              expiredTransactions.forEach(this::removeUnconfirmedTransaction);
              Account.flushAccountTable();
              Burst.getStores().commitTransaction();

            } catch (Exception e) {
              logger.error(e.toString(), e);
              Burst.getStores().rollbackTransaction();
              throw e;
            } finally {
              Burst.getStores().endTransaction();
            }
          } // synchronized
        }
      } catch (Exception e) {
        logger.debug("Error removing unconfirmed transactions", e);
      }
    } catch (Throwable t) {
      logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
      System.exit(1);
    }

  };

  private final Runnable rebroadcastTransactionsThread = () -> {

    try {
      try {
        List<Transaction> transactionList = new ArrayList<>();
        int curTime = Burst.getEpochTime();
        nonBroadcastedTransactions.forEach(transaction -> {
            if (Burst.getDbs().getTransactionDb().hasTransaction(transaction.getId()) || transaction.getExpiration() < curTime) {
                nonBroadcastedTransactions.remove(transaction);
            } else if (transaction.getTimestamp() < curTime - 30) {
                transactionList.add(transaction);
            }
          });

        if (transactionList.size() > 0) {
          Peers.rebroadcastTransactions(transactionList);
        }

      } catch (Exception e) {
        logger.debug("Error in transaction re-broadcasting thread", e);
      }
    } catch (Throwable t) {
      logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
      System.exit(1);
    }

  };

  private final Runnable processTransactionsThread = new Runnable() {

      private final JSONStreamAware getUnconfirmedTransactionsRequest;
      {
        JSONObject request = new JSONObject();
        request.put("requestType", "getUnconfirmedTransactions");
        getUnconfirmedTransactionsRequest = JSON.prepareRequest(request);
      }

      @Override
      public void run() {
        try {
          try {
            synchronized (blockchain) {
              if(lostTransactions.size() > 0) {
                List<Transaction> reAdded = processTransactions(lostTransactions, false);

                if(enableTransactionRebroadcasting && Burst.getEpochTime() - Burst.getBlockchain().getLastBlock().getTimestamp() < 4 * 60) {
                  List<Transaction> rebroadcastLost = new ArrayList<>();
                  reAdded.forEach(lost -> {
                      if (lostTransactionHeights.containsKey(lost.getId())) {
                          int addedHeight = lostTransactionHeights.get(lost.getId());
                          if (Burst.getBlockchain().getHeight() - addedHeight >= rebroadcastAfter
                                  && (Burst.getBlockchain().getHeight() - addedHeight - rebroadcastAfter) % rebroadcastEvery == 0) {
                              rebroadcastLost.add(lost);
                          }
                      } else {
                          lostTransactionHeights.put(lost.getId(), Burst.getBlockchain().getHeight());
                      }
                    });

                  for(Transaction lost : rebroadcastLost) {
                    if(!nonBroadcastedTransactions.contains(lost)) {
                      nonBroadcastedTransactions.add((TransactionImpl)lost);
                    }
                  }

                    lostTransactionHeights.keySet().removeIf(id -> getUnconfirmedTransaction(id) == null);
                }

                lostTransactions.clear();
              }
            }
            Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
            if (peer == null) {
              return;
            }
            JSONObject response = peer.send(getUnconfirmedTransactionsRequest);
            if (response == null) {
              return;
            }
            JSONArray transactionsData = (JSONArray)response.get("unconfirmedTransactions");
            if (transactionsData == null || transactionsData.isEmpty()) {
              return;
            }
            try {
              processPeerTransactions(transactionsData);
            } catch (BurstException.ValidationException|RuntimeException e) {
              peer.blacklist(e);
            }
          } catch (Exception e) {
            logger.debug("Error processing unconfirmed transactions", e);
          }
        } catch (Throwable t) {
          logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
          System.exit(1);
        }
      }

    };

  private final EconomicClustering economicClustering;
  private Blockchain blockchain;

  public TransactionProcessorImpl(LongKeyFactory<TransactionImpl> unconfirmedTransactionDbKeyFactory, EntityTable<TransactionImpl> unconfirmedTransactionTable,
      PropertyService propertyService, EconomicClustering economicClustering, Blockchain blockchain) {
    this.unconfirmedTransactionDbKeyFactory = unconfirmedTransactionDbKeyFactory;
    this.unconfirmedTransactionTable = unconfirmedTransactionTable;

    this.economicClustering = economicClustering;
    this.blockchain = blockchain;

    this.enableTransactionRebroadcasting = propertyService.getBooleanProperty("brs.enableTransactionRebroadcasting");
    this.testUnconfirmedTransactions = propertyService.getBooleanProperty("brs.testUnconfirmedTransactions");

    this.rebroadcastAfter = propertyService.getIntProperty("brs.rebroadcastAfter") != 0 ? propertyService.getIntProperty("brs.rebroadcastAfter") : 4;
    this.rebroadcastEvery = propertyService.getIntProperty("brs.rebroadcastEvery") != 0 ? propertyService.getIntProperty("brs.rebroadcastEvery") : 2;

    ThreadPool.scheduleThread("ProcessTransactions", processTransactionsThread, 5);
    ThreadPool.scheduleThread("RemoveUnconfirmedTransactions", removeUnconfirmedTransactionsThread, 1);
    if (enableTransactionRebroadcasting) {
      ThreadPool.scheduleThread("RebroadcastTransactions", rebroadcastTransactionsThread, 60);
      ThreadPool.runAfterStart(() -> {
        try (BurstIterator<TransactionImpl> oldNonBroadcastedTransactions = getAllUnconfirmedTransactions()) {
          while(oldNonBroadcastedTransactions.hasNext()) {
            nonBroadcastedTransactions.add(oldNonBroadcastedTransactions.next());
          }
        }
      });
    }
  }

  @Override
  public boolean addListener(Listener<List<? extends Transaction>> listener, Event eventType) {
    return transactionListeners.addListener(listener, eventType);
  }

  @Override
  public boolean removeListener(Listener<List<? extends Transaction>> listener, Event eventType) {
    return transactionListeners.removeListener(listener, eventType);
  }

  void notifyListeners(List<? extends Transaction> transactions, Event eventType) {
    transactionListeners.notify(transactions, eventType);
  }

  @Override
  public BurstIterator<TransactionImpl> getAllUnconfirmedTransactions() {
    return unconfirmedTransactionTable.getAll(0, -1);
  }

  @Override
  public Transaction getUnconfirmedTransaction(long transactionId) {
    return unconfirmedTransactionTable.get(unconfirmedTransactionDbKeyFactory.newKey(transactionId));
  }

  @Override
  public Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline,
                                                   Attachment attachment) {
    byte version = (byte) getTransactionVersion(Burst.getBlockchain().getHeight());
    int timestamp = Burst.getEpochTime();
    TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey, amountNQT, feeNQT, timestamp,
                                                                          deadline, (Attachment.AbstractAttachment)attachment);
    if (version > 0) {
      Block ecBlock = this.economicClustering.getECBlock(timestamp);
      builder.ecBlockHeight(ecBlock.getHeight());
      builder.ecBlockId(ecBlock.getId());
    }
    return builder;
  }

  @Override
  public void broadcast(Transaction transaction) throws BurstException.ValidationException {
    if (! transaction.verifySignature()) {
      throw new BurstException.NotValidException("Transaction signature verification failed");
    }
    List<Transaction> processedTransactions;
    synchronized (blockchain) {
      if (Burst.getDbs().getTransactionDb().hasTransaction(transaction.getId())) {
        logger.info("Transaction " + transaction.getStringId() + " already in blockchain, will not broadcast again");
        return;
      }
      if (unconfirmedTransactionTable.get(((TransactionImpl) transaction).getDbKey()) != null) {
        if (enableTransactionRebroadcasting) {
          nonBroadcastedTransactions.add((TransactionImpl) transaction);
          logger.info("Transaction " + transaction.getStringId() + " already in unconfirmed pool, will re-broadcast");
        } else {
          logger.info("Transaction " + transaction.getStringId() + " already in unconfirmed pool, will not broadcast again");
        }
        return;
      }
      processedTransactions = processTransactions(Collections.singleton((TransactionImpl) transaction), true);
    }
    if (processedTransactions.contains(transaction)) {
      if (enableTransactionRebroadcasting) {
        nonBroadcastedTransactions.add((TransactionImpl) transaction);
      }
      logger.debug("Accepted new transaction " + transaction.getStringId());
    } else {
      logger.debug("Could not accept new transaction " + transaction.getStringId());
      throw new BurstException.NotValidException("Invalid transaction " + transaction.getStringId());
    }
  }

  @Override
  public void processPeerTransactions(JSONObject request) throws BurstException.ValidationException {
    JSONArray transactionsData = (JSONArray)request.get("transactions");
    processPeerTransactions(transactionsData);
  }

  @Override
  public Transaction parseTransaction(byte[] bytes) throws BurstException.ValidationException {
    return TransactionImpl.parseTransaction(bytes);
  }

  @Override
  public TransactionImpl parseTransaction(JSONObject transactionData) throws BurstException.NotValidException {
    return TransactionImpl.parseTransaction(transactionData);
  }
    
  @Override
  public void clearUnconfirmedTransactions() {
    synchronized (blockchain) {
      List<Transaction> removed = new ArrayList<>();
      try {
        Burst.getStores().beginTransaction();

        try (BurstIterator<TransactionImpl> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
          while(unconfirmedTransactions.hasNext()) {
            TransactionImpl transaction = unconfirmedTransactions.next();
            transaction.undoUnconfirmed();
            removed.add(transaction);
          }
        }
        unconfirmedTransactionTable.truncate();
        Account.flushAccountTable();
        Burst.getStores().commitTransaction();
      } catch (Exception e) {
        logger.error(e.toString(), e);
        Burst.getStores().rollbackTransaction();

        throw e;
      } finally {
        Burst.getStores().endTransaction();
      }
      lostTransactions.clear();
      transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }
  }

  void requeueAllUnconfirmedTransactions() {
    List<Transaction> removed = new ArrayList<>();
    try (BurstIterator<TransactionImpl> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
      while(unconfirmedTransactions.hasNext()) {
        TransactionImpl transaction = unconfirmedTransactions.next();
        transaction.undoUnconfirmed();
        removed.add(transaction);
        lostTransactions.add(transaction);
      }
    }
    unconfirmedTransactionTable.truncate();
    transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
  }

  void removeUnconfirmedTransaction(TransactionImpl transaction) {
    if (!Burst.getStores().isInTransaction()) {
      synchronized (blockchain) {
        try {
          Burst.getStores().beginTransaction();
          removeUnconfirmedTransaction(transaction);
          Account.flushAccountTable();
          Burst.getStores().commitTransaction();
        } catch (Exception e) {
          logger.error(e.toString(), e);
          Burst.getStores().rollbackTransaction();
          throw e;
        } finally {
          Burst.getStores().endTransaction();
        }
      }
      return;
    }

    int deleted = Burst.getStores().getTransactionProcessorStore().deleteTransaction(transaction);
    if (deleted > 0) {
      transaction.undoUnconfirmed();
      transactionListeners.notify(Collections.singletonList(transaction), Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }
  }

  int getTransactionVersion(int previousBlockHeight) {
    return previousBlockHeight < Constants.DIGITAL_GOODS_STORE_BLOCK ? 0 : 1;
  }

  // Watch: This is not really clean
  void processLater(Collection<TransactionImpl> transactions) {
    Burst.getStores().getTransactionProcessorStore().processLater(transactions);
  }

  private void processPeerTransactions(JSONArray transactionsData) throws BurstException.ValidationException {
    if (Burst.getBlockchain().getLastBlock().getTimestamp() < Burst.getEpochTime() - 60 * 1440 && ! testUnconfirmedTransactions) {
      return;
    }
    if (Burst.getBlockchain().getHeight() <= Constants.NQT_BLOCK) {
      return;
    }
    List<TransactionImpl> transactions = new ArrayList<>();
    for (Object transactionData : transactionsData) {
      try {
        TransactionImpl transaction = parseTransaction((JSONObject) transactionData);
        transaction.validate();
        if(!this.economicClustering.verifyFork(transaction)) {
          /*if(Burst.getBlockchain().getHeight() >= Constants.EC_CHANGE_BLOCK_1) {
            throw new BurstException.NotValidException("Transaction from wrong fork");
            }*/
          continue;
        }
        transactions.add(transaction);
      } catch (BurstException.NotCurrentlyValidException ignore) {
      } catch (BurstException.NotValidException e) {
        logger.debug("Invalid transaction from peer: " + ((JSONObject) transactionData).toJSONString());
        throw e;
      }
    }
    processTransactions(transactions, true);
    nonBroadcastedTransactions.removeAll(transactions);
  }

  List<Transaction> processTransactions(Collection<TransactionImpl> transactions, final boolean sendToPeers) {
    if (transactions.isEmpty()) {
      return Collections.emptyList();
    }
    List<Transaction> sendToPeersTransactions = new ArrayList<>();
    List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
    List<Transaction> addedDoubleSpendingTransactions = new ArrayList<>();

    for (TransactionImpl transaction : transactions) {

      try {

        int curTime = Burst.getEpochTime();
        if (transaction.getTimestamp() > curTime + 15 || transaction.getExpiration() < curTime
            || transaction.getDeadline() > 1440) {
          continue;
        }
        //if (transaction.getVersion() < 1) {
        //    continue;
        //}

        synchronized (blockchain) {
          try {
            Burst.getStores().beginTransaction();
            if (Burst.getBlockchain().getHeight() < Constants.NQT_BLOCK) {
              break; // not ready to process transactions
            }

            if (Burst.getDbs().getTransactionDb().hasTransaction(transaction.getId()) || unconfirmedTransactionTable.get(transaction.getDbKey()) != null) {
              Burst.getStores().commitTransaction();
              continue;
            }

            if (!(transaction.verifySignature() && transaction.verifyPublicKey())) {
              if (Account.getAccount(transaction.getSenderId()) != null) {
                logger.debug("Transaction " + transaction.getJSONObject().toJSONString() + " failed to verify");
              }
              Burst.getStores().commitTransaction();
              continue;
            }

            if (transaction.applyUnconfirmed()) {
              if (sendToPeers) {
                if (nonBroadcastedTransactions.contains(transaction)) {
                  logger.debug("Received back transaction " + transaction.getStringId()
                               + " that we generated, will not forward to peers");
                  nonBroadcastedTransactions.remove(transaction);
                } else {
                  sendToPeersTransactions.add(transaction);
                }
              }
              unconfirmedTransactionTable.insert(transaction);
              addedUnconfirmedTransactions.add(transaction);
            } else {
              addedDoubleSpendingTransactions.add(transaction);
            }
            Account.flushAccountTable();
            Burst.getStores().commitTransaction();
          } catch (Exception e) {
            Burst.getStores().rollbackTransaction();
            throw e;
          } finally {
            Burst.getStores().endTransaction();
          }
        }
      } catch (RuntimeException e) {
        logger.info("Error processing transaction", e);
      }

    }

    if (sendToPeersTransactions.size() > 0) {
      Peers.sendToSomePeers(sendToPeersTransactions);
    }

    if (addedUnconfirmedTransactions.size() > 0) {
      transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
    }
    if (addedDoubleSpendingTransactions.size() > 0) {
      transactionListeners.notify(addedDoubleSpendingTransactions, Event.ADDED_DOUBLESPENDING_TRANSACTIONS);
    }
    return addedUnconfirmedTransactions;
  }

}
