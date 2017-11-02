package brs;

import brs.db.EntityTable;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.sql.EntitySqlTable;
import brs.peer.Peer;
import brs.peer.Peers;
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

public final class TransactionProcessorImpl implements TransactionProcessor {

  private static final Logger logger = LoggerFactory.getLogger(TransactionProcessorImpl.class);

  private static final boolean enableTransactionRebroadcasting = Nxt.getBooleanProperty("brs.enableTransactionRebroadcasting");
  private static final boolean testUnconfirmedTransactions = Nxt.getBooleanProperty("brs.testUnconfirmedTransactions");

  private static final int rebroadcastAfter = Nxt.getIntProperty("burst.rebroadcastAfter") != 0 ? Nxt.getIntProperty("burst.rebroadcastAfter") : 4;
  private static final int rebroadcastEvery = Nxt.getIntProperty("burst.rebroadcastEvery") != 0 ? Nxt.getIntProperty("burst.rebroadcastEvery") : 2;

  private static final TransactionProcessorImpl instance = new TransactionProcessorImpl();

  static TransactionProcessorImpl getInstance() {
    return instance;
  }

  final NxtKey.LongKeyFactory<TransactionImpl> unconfirmedTransactionDbKeyFactory =
      Nxt.getStores().getTransactionProcessorStore().getUnconfirmedTransactionDbKeyFactory();


  private final EntityTable<TransactionImpl> unconfirmedTransactionTable =
      Nxt.getStores().getTransactionProcessorStore().getUnconfirmedTransactionTable();

  private final Set<TransactionImpl> nonBroadcastedTransactions = Collections.newSetFromMap(new ConcurrentHashMap<TransactionImpl,Boolean>());
  private final Listeners<List<? extends Transaction>,Event> transactionListeners = new Listeners<>();
  private final Set<TransactionImpl> lostTransactions = new HashSet<>();
  private final Map<Long, Integer> lostTransactionHeights = new HashMap<>();

  private final Runnable removeUnconfirmedTransactionsThread = new Runnable() {

      @Override
      public void run() {

        try {
          try {
            List<TransactionImpl> expiredTransactions = new ArrayList<>();
            try (NxtIterator<TransactionImpl> iterator = Nxt.getStores().getTransactionProcessorStore().getExpiredTransactions()) {
              while (iterator.hasNext()) {
                expiredTransactions.add(iterator.next());
              }
            }
            if (expiredTransactions.size() > 0) {
              synchronized (BlockchainImpl.getInstance()) {
                try {
                  Nxt.getStores().beginTransaction();

                  for (TransactionImpl transaction : expiredTransactions) {
                    removeUnconfirmedTransaction(transaction);
                  }
                  Account.flushAccountTable();
                  Nxt.getStores().commitTransaction();

                } catch (Exception e) {
                  logger.error(e.toString(), e);
                  Nxt.getStores().rollbackTransaction();
                  throw e;
                } finally {
                  Nxt.getStores().endTransaction();
                }
              } // synchronized
            }
          } catch (Exception e) {
            logger.debug("Error removing unconfirmed transactions", e);
          }
        } catch (Throwable t) {
          logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
          t.printStackTrace();
          System.exit(1);
        }

      }

    };

  private final Runnable rebroadcastTransactionsThread = new Runnable() {

      @Override
      public void run() {

        try {
          try {
            List<Transaction> transactionList = new ArrayList<>();
            int curTime = Nxt.getEpochTime();
            for (TransactionImpl transaction : nonBroadcastedTransactions) {
              if (Nxt.getDbs().getTransactionDb().hasTransaction(transaction.getId()) || transaction.getExpiration() < curTime) {
                nonBroadcastedTransactions.remove(transaction);
              } else if (transaction.getTimestamp() < curTime - 30) {
                transactionList.add(transaction);
              }
            }

            if (transactionList.size() > 0) {
              Peers.rebroadcastTransactions(transactionList);
            }

          } catch (Exception e) {
            logger.debug("Error in transaction re-broadcasting thread", e);
          }
        } catch (Throwable t) {
          logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
          t.printStackTrace();
          System.exit(1);
        }

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
            synchronized (BlockchainImpl.getInstance()) {
              if(lostTransactions.size() > 0) {
                List<Transaction> reAdded = processTransactions(lostTransactions, false);

                if(enableTransactionRebroadcasting && Nxt.getEpochTime() - Nxt.getBlockchain().getLastBlock().getTimestamp() < 4 * 60) {
                  List<Transaction> rebroadcastLost = new ArrayList<>();
                  for (Transaction lost : reAdded) {
                    if (lostTransactionHeights.containsKey(lost.getId())) {
                      int addedHeight = lostTransactionHeights.get(lost.getId());
                      if (Nxt.getBlockchain().getHeight() - addedHeight >= rebroadcastAfter
                          && (Nxt.getBlockchain().getHeight() - addedHeight - rebroadcastAfter) % rebroadcastEvery == 0) {
                        rebroadcastLost.add(lost);
                      }
                    } else {
                      lostTransactionHeights.put(lost.getId(), Nxt.getBlockchain().getHeight());
                    }
                  }

                  for(Transaction lost : rebroadcastLost) {
                    if(!nonBroadcastedTransactions.contains(lost)) {
                      nonBroadcastedTransactions.add((TransactionImpl)lost);
                    }
                  }

                  Iterator<Long> it = lostTransactionHeights.keySet().iterator();
                  while(it.hasNext()) {
                    long id = it.next();
                    if(getUnconfirmedTransaction(id) == null) {
                      it.remove();
                    }
                  }
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
            if (transactionsData == null || transactionsData.size() == 0) {
              return;
            }
            try {
              processPeerTransactions(transactionsData);
            } catch (NxtException.ValidationException|RuntimeException e) {
              peer.blacklist(e);
            }
          } catch (Exception e) {
            logger.debug("Error processing unconfirmed transactions", e);
          }
        } catch (Throwable t) {
          logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
          t.printStackTrace();
          System.exit(1);
        }
      }

    };

  private TransactionProcessorImpl() {
    ThreadPool.scheduleThread("ProcessTransactions", processTransactionsThread, 5);
    ThreadPool.scheduleThread("RemoveUnconfirmedTransactions", removeUnconfirmedTransactionsThread, 1);
    if (enableTransactionRebroadcasting) {
      ThreadPool.scheduleThread("RebroadcastTransactions", rebroadcastTransactionsThread, 60);
      ThreadPool.runAfterStart(new Runnable() {
          @Override
          public void run() {
            try (NxtIterator<TransactionImpl> oldNonBroadcastedTransactions = getAllUnconfirmedTransactions()) {
              for (TransactionImpl transaction : oldNonBroadcastedTransactions) {
                nonBroadcastedTransactions.add(transaction);
              }
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
  public NxtIterator<TransactionImpl> getAllUnconfirmedTransactions() {
    return unconfirmedTransactionTable.getAll(0, -1);
  }

  @Override
  public Transaction getUnconfirmedTransaction(long transactionId) {
    return unconfirmedTransactionTable.get(unconfirmedTransactionDbKeyFactory.newKey(transactionId));
  }

  public Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline,
                                                   Attachment attachment) {
    byte version = (byte) getTransactionVersion(Nxt.getBlockchain().getHeight());
    int timestamp = Nxt.getEpochTime();
    TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey, amountNQT, feeNQT, timestamp,
                                                                          deadline, (Attachment.AbstractAttachment)attachment);
    if (version > 0) {
      Block ecBlock = EconomicClustering.getECBlock(timestamp);
      builder.ecBlockHeight(ecBlock.getHeight());
      builder.ecBlockId(ecBlock.getId());
    }
    return builder;
  }

  @Override
  public void broadcast(Transaction transaction) throws NxtException.ValidationException {
    if (! transaction.verifySignature()) {
      throw new NxtException.NotValidException("Transaction signature verification failed");
    }
    List<Transaction> processedTransactions;
    synchronized (BlockchainImpl.getInstance()) {
      if (Nxt.getDbs().getTransactionDb().hasTransaction(transaction.getId())) {
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
      throw new NxtException.NotValidException("Invalid transaction " + transaction.getStringId());
    }
  }

  @Override
  public void processPeerTransactions(JSONObject request) throws NxtException.ValidationException {
    JSONArray transactionsData = (JSONArray)request.get("transactions");
    processPeerTransactions(transactionsData);
  }

  @Override
  public Transaction parseTransaction(byte[] bytes) throws NxtException.ValidationException {
    return TransactionImpl.parseTransaction(bytes);
  }

  @Override
  public TransactionImpl parseTransaction(JSONObject transactionData) throws NxtException.NotValidException {
    return TransactionImpl.parseTransaction(transactionData);
  }
    
  @Override
  public void clearUnconfirmedTransactions() {
    synchronized (BlockchainImpl.getInstance()) {
      List<Transaction> removed = new ArrayList<>();
      try {
        Nxt.getStores().beginTransaction();

        try (NxtIterator<TransactionImpl> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
          for (TransactionImpl transaction : unconfirmedTransactions) {
            transaction.undoUnconfirmed();
            removed.add(transaction);
          }
        }
        unconfirmedTransactionTable.truncate();
        Account.flushAccountTable();
        Nxt.getStores().commitTransaction();
      } catch (Exception e) {
        logger.error(e.toString(), e);
        Nxt.getStores().rollbackTransaction();

        throw e;
      } finally {
        Nxt.getStores().endTransaction();
      }
      lostTransactions.clear();
      transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }
  }

  void requeueAllUnconfirmedTransactions() {
    List<Transaction> removed = new ArrayList<>();
    try (NxtIterator<TransactionImpl> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
      for (TransactionImpl transaction : unconfirmedTransactions) {
        transaction.undoUnconfirmed();
        removed.add(transaction);
        lostTransactions.add(transaction);
      }
    }
    unconfirmedTransactionTable.truncate();
    transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
  }

  void removeUnconfirmedTransaction(TransactionImpl transaction) {
    if (!Nxt.getStores().isInTransaction()) {
      synchronized (BlockchainImpl.getInstance()) {
        try {
          Nxt.getStores().beginTransaction();
          removeUnconfirmedTransaction(transaction);
          Account.flushAccountTable();
          Nxt.getStores().commitTransaction();
        } catch (Exception e) {
          logger.error(e.toString(), e);
          Nxt.getStores().rollbackTransaction();
          throw e;
        } finally {
          Nxt.getStores().endTransaction();
        }
      }
      return;
    }

    int deleted = Nxt.getStores().getTransactionProcessorStore().deleteTransaction(transaction);
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
    Nxt.getStores().getTransactionProcessorStore().processLater(transactions);
  }

  private void processPeerTransactions(JSONArray transactionsData) throws NxtException.ValidationException {
    if (Nxt.getBlockchain().getLastBlock().getTimestamp() < Nxt.getEpochTime() - 60 * 1440 && ! testUnconfirmedTransactions) {
      return;
    }
    if (Nxt.getBlockchain().getHeight() <= Constants.NQT_BLOCK) {
      return;
    }
    List<TransactionImpl> transactions = new ArrayList<>();
    for (Object transactionData : transactionsData) {
      try {
        TransactionImpl transaction = parseTransaction((JSONObject) transactionData);
        transaction.validate();
        if(!EconomicClustering.verifyFork(transaction)) {
          /*if(Nxt.getBlockchain().getHeight() >= Constants.EC_CHANGE_BLOCK_1) {
            throw new NxtException.NotValidException("Transaction from wrong fork");
            }*/
          continue;
        }
        transactions.add(transaction);
      } catch (NxtException.NotCurrentlyValidException ignore) {
      } catch (NxtException.NotValidException e) {
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

        int curTime = Nxt.getEpochTime();
        if (transaction.getTimestamp() > curTime + 15 || transaction.getExpiration() < curTime
            || transaction.getDeadline() > 1440) {
          continue;
        }
        //if (transaction.getVersion() < 1) {
        //    continue;
        //}

        synchronized (BlockchainImpl.getInstance()) {
          try {
            Nxt.getStores().beginTransaction();
            if (Nxt.getBlockchain().getHeight() < Constants.NQT_BLOCK) {
              break; // not ready to process transactions
            }

            if (Nxt.getDbs().getTransactionDb().hasTransaction(transaction.getId()) || unconfirmedTransactionTable.get(transaction.getDbKey()) != null) {
              Nxt.getStores().commitTransaction();
              continue;
            }

            if (!(transaction.verifySignature() && transaction.verifyPublicKey())) {
              if (Account.getAccount(transaction.getSenderId()) != null) {
                logger.debug("Transaction " + transaction.getJSONObject().toJSONString() + " failed to verify");
              }
              Nxt.getStores().commitTransaction();
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
            Nxt.getStores().commitTransaction();
          } catch (Exception e) {
            Nxt.getStores().rollbackTransaction();
            throw e;
          } finally {
            Nxt.getStores().endTransaction();
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
