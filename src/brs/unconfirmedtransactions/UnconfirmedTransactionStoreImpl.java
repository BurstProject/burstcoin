package brs.unconfirmedtransactions;

import brs.BurstException.ValidationException;
import brs.Constants;
import brs.Transaction;
import brs.db.store.AccountStore;
import brs.props.PropertyService;
import brs.props.Props;
import brs.services.TimeService;
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

/**
 * Unconfirmed Store / Handling:
 * * "Weighted FiFo": cheap transactions should be evicted first, second criteria should be the tiemstamp <- DONE
 * * max n- percent (configuration, default 5%) of the unconfirmed pool size should be allowed to have a referenced transaction full hash
 * * discard transactions, which will expire, before they come into a block, eg. if there are more than 360 lowes fee transactions in the pool we should not accept any further
 * * diff transfers
 * - each node in the net should store it's current timestamp  when it receives a unconfirmed transaction
 * - providing unconfirmed transactions (eg. getUnconfirmedTransactions get's an additional field: rxTimestamp
 * - if a node receives fetches this data from one which has the rxTimestamp it needs to store this per peer
 * - on the next request to such a peer, which has provided us with a rxTimestamp we hand over this item as a parameter, which needs to ensure, that we only get something, which is newer than the stuff we already got
 * - this design is backwards compatible, cause we only send a new field if a node provided us with the data
 *
 * Test Cases:
 * - we need to verify the memory usage (Java heap is the most important) for 8192 transactions in the memcache which have a max payload (eg. some text)
 *
 * Bugs
 * - it looks like the count for a message length is different; example:
 * On Block 50200 burst was forked to PoC2. Regarding this you need to get all of your miners PoC2 supported, the pool is no longer supporting PoC1. Update your miners to the latest version to support PoC2. And then optimize or replot your plot file to PoC2.\n\nNeed some help? Contact us on our discord server: https://discord.gg/2fnMMW4/n/nPoC2 plots are detected by not having 'staggersize' in filename.\nA PoC2 file name will look something like this: 00000000000_0_000000\n\nHeres a PoC2 plotter: https://blackpawn.com/tp//nBottom of the page is the TurboSwizzler, to optimize your plots to PoC2.\n\nLinux plotter PoC2: https://github.com/PoC-Consortium/engraver/n/nQbundle: https://github.com/PoC-Consortium/Qbundle/releases/nVersion: v2.1.0 and up\n\nBlago Miner: https://github.com/JohnnyFFM/miner-burst/releases/nVersion: v.1.170900 and up\n\ncreepMiner: https://github.com/Creepsky/creepMiner/releases/nVersion: 1.8.1 and up\n\nJminer: https://github.com/de-luxe/burstcoin-jminer/releases/nVersion: 5.2 and up"(bearbeitet)
 *
 *
 * was accepted and stored in the unconfirmed store and dropped directly during processing
 * + fee estimatior
 * + fix http api for low fees on localhost
 */

public class UnconfirmedTransactionStoreImpl implements UnconfirmedTransactionStore {

  private static final Logger logger = LoggerFactory.getLogger(UnconfirmedTransactionStoreImpl.class);

  private final TimeService timeService;
  private final ReservedBalanceCache reservedBalanceCache;

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
        final List<Transaction> expiredTransactions = getAll().stream().filter(t -> timeService.getEpochTime() > t.getExpiration()).collect(Collectors.toList());

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
  public ArrayList<Transaction> getAll() {
    synchronized (internalStore) {
      final ArrayList<Transaction> result = new ArrayList<>();

      for (List<UnconfirmedTransactionTiming> amountSlot : internalStore.values()) {
        result.addAll(amountSlot.stream().map(UnconfirmedTransactionTiming::getTransaction).collect(Collectors.toList()));
      }

      return result;
    }
  }

  @Override
  public TimedUnconfirmedTransactionOverview getAllSince(long timestampInMillis) {
    synchronized (internalStore) {
      final int currentTime = timeService.getEpochTime();

      final ArrayList<Transaction> result = new ArrayList<>();

      for (List<UnconfirmedTransactionTiming> amountSlot : internalStore.values()) {
        result.addAll(amountSlot.stream().filter(t -> t.getTimestamp() > timestampInMillis).map(UnconfirmedTransactionTiming::getTransaction).collect(Collectors.toList()));
      }

      return new TimedUnconfirmedTransactionOverview(currentTime, result);
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
