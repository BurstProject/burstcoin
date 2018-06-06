package brs.unconfirmedtransactions;

import brs.Account;
import brs.BurstException;
import brs.Transaction;
import brs.db.store.AccountStore;
import brs.props.PropertyService;
import brs.props.Props;
import brs.services.TimeService;
import brs.util.Convert;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnconfirmedTransactionStore {

  private static final Logger logger = LoggerFactory.getLogger(UnconfirmedTransactionStore.class);

  private final TimeService timeService;
  private final AccountStore accountStore;

  private final ArrayDeque<UnconfirmedTransactionTiming> idQueue;
  private final HashMap<Long, Transaction> cache;
  private final HashMap<Long, Long> reservedBalanceCache;
  private final int maxSize;

  public UnconfirmedTransactionStore(TimeService timeService, PropertyService propertyService, AccountStore accountStore) {
    this.timeService = timeService;
    this.accountStore = accountStore;

    this.maxSize = propertyService.getInt(Props.P2P_MAX_UNCONFIRMED_TRANSACTIONS);
    idQueue = new ArrayDeque<>(maxSize);
    cache = new HashMap<>(maxSize);
    reservedBalanceCache = new HashMap<>();
  }

  public void put(Collection<Transaction> transactionsToAdd) throws BurstException.ValidationException  {
    final int currentTime = timeService.getEpochTime();

    transactionsToAdd = transactionsToAdd.stream().filter(transaction -> !transactionIsExpired(transaction, currentTime)).collect(Collectors.toList());

    if (transactionsToAdd.size() > maxSize) {
      transactionsToAdd = new ArrayList<>(transactionsToAdd).subList(0, maxSize - 1);
    }

    synchronized (idQueue) {
      int amountOfTransactionsToRemove = idQueue.size() + transactionsToAdd.size() - maxSize;

      for (int i = 0; i < amountOfTransactionsToRemove; i++) {
        final UnconfirmedTransactionTiming transactionToRemoveId = idQueue.pop();
        cache.remove(transactionToRemoveId.getId());
      }

      for (Transaction transactionToAdd : transactionsToAdd) {
        if(exists(transactionToAdd.getId())) {
          remove(transactionToAdd);
        }

        idQueue.addLast(new UnconfirmedTransactionTiming(transactionToAdd.getId(), timeService.getEpochTimeMillis()));
        reserveBalanceAndPut(transactionToAdd);
      }
    }
  }

  public void put(Transaction transaction) throws BurstException.ValidationException  {
    synchronized (idQueue) {
      if (!transactionIsExpired(transaction, timeService.getEpochTime())) {
        if (idQueue.size() == maxSize) {
          final UnconfirmedTransactionTiming transactionToRemoveId = idQueue.pop();
          cache.remove(transactionToRemoveId.getId());
        } else if(exists(transaction.getId())) {
          remove(transaction);
        }

        idQueue.addLast(new UnconfirmedTransactionTiming(transaction.getId(), timeService.getEpochTimeMillis()));
        reserveBalanceAndPut(transaction);
      }
    }
  }

  public Transaction get(Long transactionId) {
    synchronized (idQueue) {
      return this.fetchUnexpiredTransactionOrCleanup(transactionId, timeService.getEpochTime());
    }
  }

  public boolean exists(Long transactionId) {
    synchronized (idQueue) {
      return this.fetchUnexpiredTransactionOrCleanup(transactionId, timeService.getEpochTime()) != null;
    }
  }

  public ArrayList<Transaction> getAll() {
    synchronized (idQueue) {
      final int currentTime = timeService.getEpochTime();
      return new ArrayList<>(cache.values().stream()
          .filter(possiblyExpired -> fetchUnexpiredTransactionOrCleanup(possiblyExpired.getId(), currentTime) != null)
          .collect(Collectors.toList()));
    }
  }

  public List<Transaction> getAllSince(long timestampInMillis) {
    synchronized (idQueue) {
      final int currentTime = timeService.getEpochTime();

      final ArrayList<UnconfirmedTransactionTiming> idQueueArray = new ArrayList(idQueue);

      int positionOfFirstTransactionSinceTimestamp = -1;
      for(int i = 0; i < idQueueArray.size(); i++) {
        if(idQueueArray.get(i).getTimestamp() > timestampInMillis) {
          positionOfFirstTransactionSinceTimestamp = i;
          break;
        }
      }

      if(positionOfFirstTransactionSinceTimestamp > -1) {
        return idQueueArray.stream()
            .skip(positionOfFirstTransactionSinceTimestamp)
            .map(utt -> cache.get(utt.getId()))
            .filter(possiblyExpired -> fetchUnexpiredTransactionOrCleanup(possiblyExpired.getId(), currentTime) != null)
            .collect(Collectors.toList());
      }

      return new ArrayList<>();
    }
  }

  public void forEach(Consumer<Transaction> consumer) {
    synchronized (idQueue) {
      final int currentTime = timeService.getEpochTime();

      for (Iterator<Transaction> it = cache.values().iterator(); it.hasNext();) {
        Transaction t = it.next();
        if(transactionIsExpired(t, currentTime)) {
          it.remove();
          idQueue.removeFirstOccurrence(t.getId());
          refundBalance(t);
        }
      }

      cache.values().stream().forEach(consumer);
    }
  }

  public void remove(Transaction transaction) {
    synchronized (idQueue) {
      if (exists(transaction.getId())) {
        removeTransaction(transaction);
      }
    }
  }

  public void clear() {
    synchronized (idQueue) {
      idQueue.clear();
      cache.clear();
      reservedBalanceCache.clear();
    }
  }

  private Transaction fetchUnexpiredTransactionOrCleanup(Long transactionId, int currentTime) {
    final Transaction possibleTransaction = cache.get(transactionId);

    if (possibleTransaction != null) {
      if (!transactionIsExpired(possibleTransaction, currentTime)) {
        return possibleTransaction;
      } else {
        removeTransaction(possibleTransaction);
      }
    }

    return null;
  }

  private void removeTransaction(Transaction transaction) {
    idQueue.removeFirstOccurrence(transaction.getId());
    cache.remove(transaction.getId());
    refundBalance(transaction);
  }

  private boolean transactionIsExpired(Transaction transaction, int currentTime) {
    return transaction.getExpiration() < currentTime;
  }

  private void reserveBalanceAndPut(Transaction transaction) throws BurstException.ValidationException {
    // I think in theory a put could be made with for the same transaction id with a different amount/fee
    // so we refund the existing ones balance before we do a fresh reserve
    if ( cache.containsKey(transaction.getId()) ) {
      refundBalance(transaction);
    }

    Account senderAccount = null;
    if(transaction.getSenderId() != 0) {
      senderAccount = accountStore.getAccountTable().get(accountStore.getAccountKeyFactory().newKey(transaction.getSenderId()));
    }

    final Long amountNQT = Convert.safeAdd(
        reservedBalanceCache.getOrDefault(transaction.getSenderId(), 0L),
        transaction.getType().calculateTotalAmountNQT(transaction)
    );

    if (senderAccount == null) {
      logger.debug(String.format("Account %d does not exist and has no balance. Required funds: %d",transaction.getSenderId(), amountNQT));

      throw new BurstException.NotCurrentlyValidException("Account unknown");
    } else if ( amountNQT > senderAccount.getUnconfirmedBalanceNQT() ) {
      logger.debug(String.format("Account %d balance to low. You have  %d > %d Balance",
          transaction.getSenderId(), amountNQT, senderAccount.getUnconfirmedBalanceNQT()
      ));

      throw new BurstException.NotCurrentlyValidException("Insufficient funds");
    }

    reservedBalanceCache.put(transaction.getSenderId(), amountNQT);
    cache.put(transaction.getId(), transaction);
  }

  private void refundBalance(Transaction transaction) {
    Long amountNQT = Convert.safeSubtract(
        reservedBalanceCache.getOrDefault(transaction.getSenderId(), 0L),
        transaction.getType().calculateTotalAmountNQT(transaction)
    );

    if (amountNQT > 0) {
      reservedBalanceCache.put(transaction.getSenderId(), amountNQT);
    } else {
      reservedBalanceCache.remove(transaction.getSenderId());
    }
  }

}
