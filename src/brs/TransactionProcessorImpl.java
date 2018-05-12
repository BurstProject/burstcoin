package brs;

import brs.common.Props;
import brs.db.BurstKey;
import brs.db.store.Dbs;
import brs.db.store.Stores;
import brs.fluxcapacitor.FeatureToggle;
import brs.peer.Peer;
import brs.peer.Peers;
import brs.services.AccountService;
import brs.services.PropertyService;
import brs.services.TimeService;
import brs.services.TransactionService;
import brs.util.JSON;
import brs.util.Listener;
import brs.util.Listeners;
import brs.util.ThreadPool;
import org.ehcache.Cache;
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

  private final Object unconfirmedTransactionsSyncObj = new Object();
  
  private final Set<Transaction> nonBroadcastedTransactions = Collections.newSetFromMap(new ConcurrentHashMap<Transaction,Boolean>());
  private final Listeners<List<? extends Transaction>,Event> transactionListeners = new Listeners<>();
  private final Set<Transaction> lostTransactions = new HashSet<>();
  private final Map<Long, Integer> lostTransactionHeights = new HashMap<>();

  private final EconomicClustering economicClustering;
  private Stores stores;
  private TimeService timeService;
  private TransactionService transactionService;
  private Dbs dbs;
  private Blockchain blockchain;
  private AccountService accountService;
  private UnconfirmedTransactionStore unconfirmedTransactionStore;

  public TransactionProcessorImpl(PropertyService propertyService,
      EconomicClustering economicClustering, Blockchain blockchain, Stores stores, TimeService timeService, Dbs dbs, AccountService accountService,
      TransactionService transactionService, ThreadPool threadPool) {

    this.economicClustering = economicClustering;
    this.blockchain = blockchain;
    this.timeService = timeService;

    this.stores = stores;
    this.dbs = dbs;

    this.accountService = accountService;
    this.transactionService = transactionService;

    this.enableTransactionRebroadcasting = propertyService.getBoolean(Props.P2P_ENABLE_TX_REBROADCAST);
    this.testUnconfirmedTransactions = propertyService.getBoolean(Props.BRS_TEST_UNCONFIRMED_TRANSACTIONS);

    this.rebroadcastAfter = propertyService.getInt(Props.P2P_REBROADCAST_AFTER, 4);
    this.rebroadcastEvery = propertyService.getInt(Props.P2P_REBROADCAST_EVERY, 2);

    this.unconfirmedTransactionStore = stores.getUnconfirmedTransactionStore();
    threadPool.scheduleThread("ProcessTransactions", processTransactionsThread, 5);
    if (enableTransactionRebroadcasting) {
      threadPool.scheduleThread("RebroadcastTransactions", rebroadcastTransactionsThread, 60);
      threadPool.runAfterStart(() -> {
        unconfirmedTransactionStore.forEach(transaction -> {
          nonBroadcastedTransactions.add(transaction);
        });
      });
    }
  }
  public Object getUnconfirmedTransactionsSyncObj() {
	  return unconfirmedTransactionsSyncObj;
  }
  private final Runnable rebroadcastTransactionsThread = () -> {

    try {
      try {
        List<Transaction> transactionList = new ArrayList<>();
        int curTime = timeService.getEpochTime();
        nonBroadcastedTransactions.forEach(transaction -> {
          if (dbs.getTransactionDb().hasTransaction(transaction.getId()) || transaction.getExpiration() < curTime) {
            nonBroadcastedTransactions.remove(transaction);
          } else if (transaction.getTimestamp() < curTime - 30) {
            transactionList.add(transaction);
          }
        });
        //executor shutdown? 
        if (Thread.currentThread().isInterrupted()) {
          return;
        }
        if (! transactionList.isEmpty()) {
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
        	  synchronized (unconfirmedTransactionsSyncObj) {
              if(! lostTransactions.isEmpty()) {
                List<Transaction> reAdded = processTransactions(lostTransactions, false);

                if(enableTransactionRebroadcasting && timeService.getEpochTime() - blockchain.getLastBlock().getTimestamp() < 4 * 60) {
                  List<Transaction> rebroadcastLost = new ArrayList<>();
                  reAdded.forEach(lost -> {
                      if (lostTransactionHeights.containsKey(lost.getId())) {
                          int addedHeight = lostTransactionHeights.get(lost.getId());
                          if (blockchain.getHeight() - addedHeight >= rebroadcastAfter
                                  && (blockchain.getHeight() - addedHeight - rebroadcastAfter) % rebroadcastEvery == 0) {
                              rebroadcastLost.add(lost);
                          }
                      } else {
                          lostTransactionHeights.put(lost.getId(), blockchain.getHeight());
                      }
                    });

                  for(Transaction lost : rebroadcastLost) {
                    if(!nonBroadcastedTransactions.contains(lost)) {
                      nonBroadcastedTransactions.add((Transaction)lost);
                    }
                  }

                    lostTransactionHeights.keySet().removeIf(id -> getUnconfirmedTransaction(id) == null);
                }

                lostTransactions.clear();
              }
            Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED);
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
  public ArrayList<Transaction> getAllUnconfirmedTransactions() {
    return unconfirmedTransactionStore.getAll();
  }

  @Override
  public Transaction getUnconfirmedTransaction(long transactionId) {
    return unconfirmedTransactionStore.get(transactionId);
  }

  @Override
  public Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline,
                                                   Attachment attachment) {
    byte version = (byte) getTransactionVersion(blockchain.getHeight());
    int timestamp = timeService.getEpochTime();
    Transaction.Builder builder = new Transaction.Builder(version, senderPublicKey, amountNQT, feeNQT, timestamp,
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
    if (dbs.getTransactionDb().hasTransaction(transaction.getId())) {
      logger.info("Transaction " + transaction.getStringId() + " already in blockchain, will not broadcast again");
      return;
    }
    if (unconfirmedTransactionStore.exists(transaction.getId())) {
      if (enableTransactionRebroadcasting) {
        nonBroadcastedTransactions.add((Transaction) transaction);
        logger.info("Transaction " + transaction.getStringId() + " already in unconfirmed pool, will re-broadcast");
      } else {
        logger.info("Transaction " + transaction.getStringId() + " already in unconfirmed pool, will not broadcast again");
      }
      return;
    }
    processedTransactions = processTransactions(Collections.singleton((Transaction) transaction), true);

    if (processedTransactions.contains(transaction)) {
      if (enableTransactionRebroadcasting) {
        nonBroadcastedTransactions.add((Transaction) transaction);
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
    return Transaction.parseTransaction(bytes);
  }

  @Override
  public Transaction parseTransaction(JSONObject transactionData) throws BurstException.NotValidException {
    return Transaction.parseTransaction(transactionData);
  }
    
  @Override
  public void clearUnconfirmedTransactions() {
	synchronized (unconfirmedTransactionsSyncObj) {
    List<Transaction> removed = new ArrayList<>();
    try {
      stores.beginTransaction();
      unconfirmedTransactionStore.forEach(
          transaction -> {
            transactionService.undoUnconfirmed(transaction);
            removed.add(transaction);
          }
      );
      accountService.flushAccountTable();
      unconfirmedTransactionStore.clear();
      stores.commitTransaction();
    } catch (Exception e) {
      logger.error(e.toString(), e);
      stores.rollbackTransaction();
      throw e;
    } finally {
      stores.endTransaction();
    }
    lostTransactions.clear();
    transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
	}
  }

  void requeueAllUnconfirmedTransactions() {
    synchronized (unconfirmedTransactionsSyncObj) {
      List<Transaction> removed = new ArrayList<>();
      unconfirmedTransactionStore.forEach(
          transaction -> {
            removed.add(transaction);
            lostTransactions.add(transaction);
          }
      );
      unconfirmedTransactionStore.clear();
      transactionListeners.notify(removed, Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }
  }
  int getTransactionVersion(int previousBlockHeight) {
    return Burst.getFluxCapacitor().isActive(FeatureToggle.DIGITAL_GOODS_STORE, previousBlockHeight) ? 1 : 0;
  }

  // Watch: This is not really clean
  void processLater(Collection<Transaction> transactions) {
    unconfirmedTransactionStore.put(transactions);
  }

  private void processPeerTransactions(JSONArray transactionsData) throws BurstException.ValidationException {
	
	if (blockchain.getLastBlock().getTimestamp() < timeService.getEpochTime() - 60 * 1440 && ! testUnconfirmedTransactions) {
      return;
    }
    if (blockchain.getHeight() <= Constants.NQT_BLOCK) {
      return;
    }
    List<Transaction> transactions = new ArrayList<>();
    for (Object transactionData : transactionsData) {
      try {
        Transaction transaction = parseTransaction((JSONObject) transactionData);
        transactionService.validate(transaction);
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

  List<Transaction> processTransactions(Collection<Transaction> transactions, final boolean sendToPeers) {
	synchronized (unconfirmedTransactionsSyncObj) {
    if (transactions.isEmpty()) {
      return Collections.emptyList();
    }
    List<Transaction> sendToPeersTransactions = new ArrayList<>();
    List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();

    for (Transaction transaction : transactions) {

      try {
        int curTime = timeService.getEpochTime();
        if (transaction.getTimestamp() > curTime + 15 || transaction.getExpiration() < curTime
            || transaction.getDeadline() > 1440) {
          continue;
        }
      
        try {
          stores.beginTransaction();
          if (blockchain.getHeight() < Constants.NQT_BLOCK) {
            break; // not ready to process transactions
          }

          if (dbs.getTransactionDb().hasTransaction(transaction.getId()) || unconfirmedTransactionStore.exists(transaction.getId())) {
            stores.commitTransaction();
            continue;
          }

          if (!(transaction.verifySignature() && transactionService.verifyPublicKey(transaction))) {
            if (accountService.getAccount(transaction.getSenderId()) != null) {
              logger.debug("Transaction " + transaction.getJSONObject().toJSONString() + " failed to verify");
            }
            stores.commitTransaction();
            continue;
          }

          if (sendToPeers) {
            if (nonBroadcastedTransactions.contains(transaction)) {
              logger.debug("Received back transaction " + transaction.getStringId()
                             + " that we generated, will not forward to peers");
              nonBroadcastedTransactions.remove(transaction);
            }
            else {
              sendToPeersTransactions.add(transaction);
            }
          }
          unconfirmedTransactionStore.put(transaction);
          addedUnconfirmedTransactions.add(transaction);
          stores.commitTransaction();
        } catch (Exception e) {
          stores.rollbackTransaction();
          throw e;
        } finally {
          stores.endTransaction();
        }
      } catch (RuntimeException e) {
        logger.info("Error processing transaction", e);
      }
    }

    if (! sendToPeersTransactions.isEmpty()) {
      Peers.sendToSomePeers(sendToPeersTransactions);
    }

    if (! addedUnconfirmedTransactions.isEmpty()) {
      transactionListeners.notify(addedUnconfirmedTransactions, Event.ADDED_UNCONFIRMED_TRANSACTIONS);
    }

    return addedUnconfirmedTransactions;
  }
  }
}
