package brs.unconfirmedtransactions;

import brs.BurstException.ValidationException;
import brs.Constants;
import brs.Transaction;
import brs.db.store.AccountStore;
import brs.props.PropertyService;
import brs.props.Props;
import brs.services.TimeService;
import brs.transactionduplicates.TransactionDuplicatesCheckerImpl;
import brs.transactionduplicates.TransactionDuplicationResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnconfirmedTransactionStoreImpl implements UnconfirmedTransactionStore {

  private static final Logger logger = LoggerFactory.getLogger(UnconfirmedTransactionStoreImpl.class);

  private final TimeService timeService;
  private final ReservedBalanceCache reservedBalanceCache;
  private final TransactionDuplicatesCheckerImpl transactionDuplicatesChecker = new TransactionDuplicatesCheckerImpl();

  private final SortedMap<Long, List<UnconfirmedTransactionTiming>> internalStore;

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private int totalSize;
  private final int maxSize;

  private int numberUnconfirmedTransactionsFullHash;
  private final int maxPercentageUnconfirmedTransactionsFullHash;

  final Runnable cleanupExpiredTransactions = new Runnable() {
    @Override
    public void run() {
      synchronized (internalStore) {
        final List<Transaction> expiredTransactions = getAll(Integer.MAX_VALUE).getTransactions().stream().filter(t -> timeService.getEpochTime() > t.getExpiration()).collect(Collectors.toList());

        expiredTransactions.stream().forEach(t -> removeTransaction(t));
      }
    }
  };

  public UnconfirmedTransactionStoreImpl(TimeService timeService, PropertyService propertyService, AccountStore accountStore) {
    this.timeService = timeService;

    this.reservedBalanceCache = new ReservedBalanceCache(accountStore);

    this.maxSize = propertyService.getInt(Props.P2P_MAX_UNCONFIRMED_TRANSACTIONS);
    this.totalSize = 0;

    this.maxPercentageUnconfirmedTransactionsFullHash = propertyService.getInt(Props.P2P_MAX_PERCENTAGE_UNCONFIRMED_TRANSACTIONS_FULL_HASH_REFERENCE);
    this.numberUnconfirmedTransactionsFullHash = 0;

    internalStore = new TreeMap<>();

    scheduler.scheduleWithFixedDelay(cleanupExpiredTransactions, 1, 1, TimeUnit.MINUTES);
  }

  @Override
  public void put(Transaction transaction) throws ValidationException {
    synchronized (internalStore) {
      if (transactionCanBeAddedToCache(transaction)) {
        final TransactionDuplicationResult duplicationInformation = transactionDuplicatesChecker.removeCheaperDuplicate(transaction);

        if(duplicationInformation.isDuplicate() && duplicationInformation.getTransaction() != transaction) {
          removeTransaction(duplicationInformation.getTransaction());
        }

        addTransaction(transaction, timeService.getEpochTimeMillis());

        if (totalSize > maxSize) {
          removeCheapestFirstToExpireTransaction();
        }
      }
    }
  }

  private boolean transactionCanBeAddedToCache(Transaction transaction) {
    return transactionIsCurrentlyValid(transaction)
        && ! transactionIsCurrentlyInCache(transaction)
        && ! cacheFullAndTransactionCheaperThanAllTheRest(transaction)
        && ! tooManyTransactionsWithReferencedFullHash(transaction)
        && ! tooManyTransactionsForSlotSize(transaction);
  }

  private boolean tooManyTransactionsForSlotSize(Transaction transaction) {
    final long slotHeight = this.amountSlotForTransaction(transaction);

    return this.internalStore.containsKey(slotHeight) && this.internalStore.get(slotHeight).size() == slotHeight * 360;
  }

  private boolean tooManyTransactionsWithReferencedFullHash(Transaction transaction) {
    return ! StringUtils.isEmpty(transaction.getReferencedTransactionFullHash()) && maxPercentageUnconfirmedTransactionsFullHash <= (((numberUnconfirmedTransactionsFullHash + 1) * 100) / maxSize);
  }

  private boolean cacheFullAndTransactionCheaperThanAllTheRest(Transaction transaction) {
    return totalSize == maxSize && internalStore.firstKey() > amountSlotForTransaction(transaction);
  }

  @Override
  public Transaction get(Long transactionId) {
    synchronized (internalStore) {
      for (List<UnconfirmedTransactionTiming> amountSlot : internalStore.values()) {
        for (UnconfirmedTransactionTiming t : amountSlot) {
          if (t.getTransaction().getId() == transactionId) {
            return t.getTransaction();
          }
        }
      }

      return null;
    }
  }

  @Override
  public boolean exists(Long transactionId) {
    synchronized (internalStore) {
      return get(transactionId) != null;
    }
  }

  @Override
  public TimedUnconfirmedTransactionOverview getAll(int limit) {
    synchronized (internalStore) {
      final ArrayList<UnconfirmedTransactionTiming> flatTransactionList = new ArrayList<>();

      for (List<UnconfirmedTransactionTiming> amountSlot : internalStore.values()) {
        flatTransactionList.addAll(amountSlot);
      }

      final List<UnconfirmedTransactionTiming> result = flatTransactionList.stream()
          .sorted(Comparator.comparingLong(UnconfirmedTransactionTiming::getTimestamp))
          .limit(limit)
          .collect(Collectors.toList());

      if(! result.isEmpty()) {
        return new TimedUnconfirmedTransactionOverview(result.get(result.size() - 1).getTimestamp(), result.stream().map(UnconfirmedTransactionTiming::getTransaction).collect(Collectors.toList()));
      } else {
        return new TimedUnconfirmedTransactionOverview(timeService.getEpochTimeMillis(), new ArrayList<>());
      }
    }
  }

  @Override
  public TimedUnconfirmedTransactionOverview getAllSince(long timestampInMillis, int limit) {
    synchronized (internalStore) {
      final ArrayList<UnconfirmedTransactionTiming> flatTransactionList = new ArrayList<>();

      for (List<UnconfirmedTransactionTiming> amountSlot : internalStore.values()) {
        flatTransactionList.addAll(amountSlot.stream().filter(t -> t.getTimestamp() > timestampInMillis).collect(Collectors.toList()));
      }

      final List<UnconfirmedTransactionTiming> result = flatTransactionList.stream()
          .sorted(Comparator.comparingLong(UnconfirmedTransactionTiming::getTimestamp))
          .limit(limit)
          .collect(Collectors.toList());

      if(! result.isEmpty()) {
        return new TimedUnconfirmedTransactionOverview(result.get(result.size() - 1).getTimestamp(), result.stream().map(UnconfirmedTransactionTiming::getTransaction).collect(Collectors.toList()));
      } else {
        return new TimedUnconfirmedTransactionOverview(timeService.getEpochTimeMillis(), new ArrayList<>());
      }
    }
  }

  @Override
  public void forEach(Consumer<Transaction> consumer) {
    synchronized (internalStore) {
      for (List<UnconfirmedTransactionTiming> amountSlot : internalStore.values()) {
        amountSlot.stream().map(UnconfirmedTransactionTiming::getTransaction).forEach(consumer);
      }
    }
  }

  @Override
  public void remove(Transaction transaction) {
    synchronized (internalStore) {
      if (exists(transaction.getId())) {
        removeTransaction(transaction);
      }
    }
  }

  @Override
  public void clear() {
    synchronized (internalStore) {
      totalSize = 0;
      internalStore.clear();
      reservedBalanceCache.clear();
      transactionDuplicatesChecker.clear();
    }
  }

  private boolean transactionIsCurrentlyInCache(Transaction transaction) {
    final List<UnconfirmedTransactionTiming> amountSlot = internalStore.get(amountSlotForTransaction(transaction));
    return amountSlot != null && amountSlot.stream().anyMatch(t -> t.getTransaction().getId() == transaction.getId());
  }

  private void addTransaction(Transaction transaction, long time) throws ValidationException {
    final List<UnconfirmedTransactionTiming> slot = getOrCreateAmountSlotForTransaction(transaction);
    slot.add(new UnconfirmedTransactionTiming(transaction, time));
    totalSize++;

    if(! StringUtils.isEmpty(transaction.getReferencedTransactionFullHash())) {
      numberUnconfirmedTransactionsFullHash++;
    }

    this.reservedBalanceCache.reserveBalanceAndPut(transaction);
  }

  private List<UnconfirmedTransactionTiming> getOrCreateAmountSlotForTransaction(Transaction transaction) {
    final long amountSlotNumber = amountSlotForTransaction(transaction);

    if (!this.internalStore.containsKey(amountSlotNumber)) {
      this.internalStore.put(amountSlotNumber, new ArrayList<>());
    }

    return this.internalStore.get(amountSlotNumber);
  }


  private long amountSlotForTransaction(Transaction transaction) {
    return transaction.getFeeNQT() / Constants.FEE_QUANT;
  }

  private void removeCheapestFirstToExpireTransaction() {
    this.internalStore.get(this.internalStore.firstKey()).stream()
        .map(UnconfirmedTransactionTiming::getTransaction)
        .sorted(Comparator.comparingLong(Transaction::getFeeNQT).thenComparing(Transaction::getExpiration).thenComparing(Transaction::getId))
        .findFirst().ifPresent(t -> removeTransaction(t));
  }

  private boolean transactionIsCurrentlyValid(Transaction transaction) {
    return timeService.getEpochTime() < transaction.getExpiration();
  }

  private void removeTransaction(Transaction transaction) {
    final long amountSlotNumber = amountSlotForTransaction(transaction);

    final List<UnconfirmedTransactionTiming> amountSlot = internalStore.get(amountSlotNumber);

    final Iterator<UnconfirmedTransactionTiming> transactionSlotIterator = amountSlot.iterator();

    while (transactionSlotIterator.hasNext()) {
      final UnconfirmedTransactionTiming utt = transactionSlotIterator.next();
      if (utt.getTransaction().getId() == transaction.getId()) {
        transactionSlotIterator.remove();
        this.reservedBalanceCache.refundBalance(transaction);
        totalSize--;

        if(! StringUtils.isEmpty(transaction.getReferencedTransactionFullHash())) {
          numberUnconfirmedTransactionsFullHash--;
        }
        return;
      }
    }

    if (amountSlot.isEmpty()) {
      this.internalStore.remove(amountSlotNumber);
    }
  }

}
