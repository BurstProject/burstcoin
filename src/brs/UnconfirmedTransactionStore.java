package brs;

import brs.common.Props;
import brs.db.store.AccountStore;
import brs.services.AccountService;
import brs.services.PropertyService;
import brs.services.TimeService;
import brs.util.Convert;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class UnconfirmedTransactionStore {

  private final TimeService timeService;
  private final AccountStore accountStore;

  private final ArrayDeque<Long> idQueue;
  private final HashMap<Long, Transaction> cache;
  private final HashMap<Long, Long> reservedBalanceCache;
  private final int maxSize;

  public UnconfirmedTransactionStore(TimeService timeService, PropertyService propertyService, AccountStore accountStore) {
    this.timeService = timeService;
    this.accountStore = accountStore;

    this.maxSize = propertyService.getInt(Props.P2P_MAX_UNCONFIRMED_TRANSACTIONS, 8192);
    idQueue = new ArrayDeque<>(maxSize);
    cache = new HashMap<>(maxSize);
    reservedBalanceCache = new HashMap<>();
  }

  private void reserveBalanceAndPut(Transaction transaction) throws BurstException.ValidationException {
    // I think in theory a put could be made with for the same transaction id with a different amount/fee
    // so we refund the existing ones balance before we do a fresh reserve
    if ( cache.containsKey(transaction.getId()) ) {
      refundBalance(transaction);
    }
    Account senderAccount = transaction.getSenderId() == 0
        ? null
        : accountStore.getAccountTable().get(
            accountStore.getAccountKeyFactory().newKey(transaction.getSenderId())
        );
    Long amountNQT = Convert.safeAdd(
        reservedBalanceCache.getOrDefault(transaction.getSenderId(), 0L),
        Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT())
    );
    if ( senderAccount == null ) {
      throw new BurstException.NotCurrentlyValidException(String.format("Account %d does not exist and has no balance. Require %d > %d Balance",
          transaction.getSenderId(), amountNQT, 0
      ));
    }
    else if ( amountNQT > senderAccount.getUnconfirmedBalanceNQT() ) {
      throw new BurstException.NotCurrentlyValidException(String.format("Account %d balance to low. Require %d > %d Balance",
          transaction.getSenderId(), amountNQT, senderAccount.getUnconfirmedBalanceNQT()
      ));
    }
    reservedBalanceCache.put(transaction.getSenderId(), amountNQT);
    cache.put(transaction.getId(), transaction);
  }

  private void refundBalance(Transaction transaction) {
    Long amountNQT = Convert.safeSubtract(
        reservedBalanceCache.getOrDefault(transaction.getSenderId(), 0L),
        Convert.safeAdd(transaction.getAmountNQT(), transaction.getFeeNQT())
    );
    if ( amountNQT > 0 ) {
      reservedBalanceCache.put(transaction.getSenderId(), amountNQT);
    }
    else {
      reservedBalanceCache.remove(transaction.getSenderId());
    }
  }

  public void close() {
    //NOOP
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
        final Long transactionToRemoveId = idQueue.pop();
        cache.remove(transactionToRemoveId);
      }

      for (Transaction transactionToAdd : transactionsToAdd) {
        idQueue.addLast(transactionToAdd.getId());
        reserveBalanceAndPut(transactionToAdd);
      }
    }
  }


  public void put(Transaction transaction) throws BurstException.ValidationException  {
    synchronized (idQueue) {
      if (!transactionIsExpired(transaction, timeService.getEpochTime())) {
        if (idQueue.size() == maxSize) {
          final Long transactionToRemoveId = idQueue.pop();
          cache.remove(transactionToRemoveId);
        }

        idQueue.addLast(transaction.getId());
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

  public void forEach(Consumer<Transaction> consumer) {
    synchronized (idQueue) {
      final int currentTime = timeService.getEpochTime();

      for (Iterator<Transaction> it = cache.values().iterator(); it.hasNext();) {
        Transaction t = it.next();
        if(transactionIsExpired(t, currentTime)) {
          remove(t);
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
  }

  private boolean transactionIsExpired(Transaction transaction, int currentTime) {
    return transaction.getExpiration() < currentTime;
  }
}
