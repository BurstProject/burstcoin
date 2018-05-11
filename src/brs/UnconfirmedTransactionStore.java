package brs;

import brs.services.TimeService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class UnconfirmedTransactionStore {

  private final TimeService timeService;

  private final ArrayDeque<Long> idQueue;
  private final HashMap<Long, Transaction> cache;
  private final int maxSize;

  public UnconfirmedTransactionStore(TimeService timeService) {
    this.timeService = timeService;

    this.maxSize = 8192;
    idQueue = new ArrayDeque<>(maxSize);
    cache = new HashMap<>(maxSize);
  }

  public void close() {
    //NOOP
  }

  public void put(Collection<Transaction> transactionsToAdd) {
    int currentTime = timeService.getEpochTime();

    transactionsToAdd = transactionsToAdd.stream().filter(transaction -> !transactionIsExpired(transaction, currentTime)).collect(Collectors.toList());

    if (transactionsToAdd.size() > maxSize) {
      transactionsToAdd = new ArrayList<>(transactionsToAdd).subList(0, maxSize - 1);
    }

    synchronized (idQueue) {
      int amountOfTransactionsToRemove = maxSize - transactionsToAdd.size();

      for (int i = 0; i < amountOfTransactionsToRemove; i++) {
        final Long transactionToRemoveId = idQueue.pop();
        cache.remove(transactionToRemoveId);
      }

      for (Transaction transactionToAdd : transactionsToAdd) {
        idQueue.addLast(transactionToAdd.getId());
        cache.put(transactionToAdd.getId(), transactionToAdd);
      }
    }
  }


  public void put(Transaction transaction) {
    synchronized (idQueue) {
      if (!transactionIsExpired(transaction, timeService.getEpochTime())) {
        if (idQueue.size() == maxSize) {
          final Long transactionToRemoveId = idQueue.pop();
          cache.remove(transactionToRemoveId);
        }

        idQueue.addLast(transaction.getId());
        cache.put(transaction.getId(), transaction);
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
      cache.values().stream()
          .filter(possiblyExpired -> fetchUnexpiredTransactionOrCleanup(possiblyExpired.getId(), currentTime) != null)
          .forEach(consumer);
    }
  }

  public void remove(Transaction transaction) {
    synchronized (idQueue) {
      if (exists(transaction.getId())) {
        idQueue.removeFirstOccurrence(transaction.getId());
        cache.remove(transaction.getId());
      }
    }
  }

  public void clear() {
    synchronized (idQueue) {
      idQueue.clear();
      cache.clear();
    }
  }

  private Transaction fetchUnexpiredTransactionOrCleanup(Long transactionId, int currentTime) {
    final Transaction possibleTransaction = cache.get(transactionId);

    if (possibleTransaction != null) {
      if (!transactionIsExpired(possibleTransaction, currentTime)) {
        return possibleTransaction;
      } else {
        idQueue.removeFirstOccurrence(transactionId);
        cache.remove(transactionId);
      }
    }

    return null;
  }

  private boolean transactionIsExpired(Transaction transaction, int currentTime) {
    return transaction.getExpiration() < currentTime;
  }
}
