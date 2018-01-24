package brs;

import brs.BurstException.NotValidException;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;

import brs.util.Convert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Subscription {

  // WATCH
  private final TransactionDb transactionDb = Burst.getDbs().getTransactionDb();
  public static boolean isEnabled() {
    if(Burst.getBlockchain().getLastBlock().getHeight() >= Constants.BURST_SUBSCRIPTION_START_BLOCK) {
      return true;
    }

    Alias subscriptionEnabled = Alias.getAlias("featuresubscription");
    if(subscriptionEnabled != null && subscriptionEnabled.getAliasURI().equals("enabled")) {
      return true;
    }

    return false;
  }

  private static final BurstKey.LongKeyFactory<Subscription> subscriptionDbKeyFactory() {
    return Burst.getStores().getSubscriptionStore().getSubscriptionDbKeyFactory();
  }

  private static final VersionedEntityTable<Subscription> subscriptionTable() {
    return Burst.getStores().getSubscriptionStore().getSubscriptionTable();
  }

  private static final List<TransactionImpl> paymentTransactions = new ArrayList<>();
  private static final List<Subscription> appliedSubscriptions = new ArrayList<>();
  private static final Set<Long> removeSubscriptions = new HashSet<>();

  public static long getFee() {
    return Constants.ONE_BURST;
  }

  public static Subscription getSubscription(Long id) {
    return subscriptionTable().get(subscriptionDbKeyFactory().newKey(id));
  }

  public static void addSubscription(Account sender,
                                     Account recipient,
                                     Long id,
                                     Long amountNQT,
                                     int startTimestamp,
                                     int frequency) {
    Subscription subscription = new Subscription(sender.getId(),
                                                 recipient.getId(),
                                                 id,
                                                 amountNQT,
                                                 frequency,
                                                 startTimestamp);

    subscriptionTable().insert(subscription);
  }

  public static void removeSubscription(Long id) {
    Subscription subscription = subscriptionTable().get(subscriptionDbKeyFactory().newKey(id));
    if(subscription != null) {
      subscriptionTable().delete(subscription);
    }
  }



  @SuppressWarnings("static-access")
  public static long calculateFees(int timestamp) {
    long totalFeeNQT = 0;
    BurstIterator<Subscription> updateSubscriptions =
        Burst.getStores().getSubscriptionStore().getUpdateSubscriptions(timestamp);
    List<Subscription> appliedUnconfirmedSubscriptions = new ArrayList<>();
    while(updateSubscriptions.hasNext()) {
      Subscription subscription = updateSubscriptions.next();
      if(removeSubscriptions.contains(subscription.getId())) {
        continue;
      }
      if(subscription.applyUnconfirmed()) {
        appliedUnconfirmedSubscriptions.add(subscription);
      }
    }
    if(appliedUnconfirmedSubscriptions.size() > 0) {
      for(Subscription subscription : appliedUnconfirmedSubscriptions) {
        totalFeeNQT = Convert.safeAdd(totalFeeNQT, subscription.getFee());
        subscription.undoUnconfirmed();
      }
    }
    return totalFeeNQT;
  }

  public static void clearRemovals() {
    removeSubscriptions.clear();
  }

  public static void addRemoval(Long id) {
    removeSubscriptions.add(id);
  }

  @SuppressWarnings("static-access")
  public static long applyUnconfirmed(int timestamp) {
    appliedSubscriptions.clear();
    long totalFees = 0;
    BurstIterator<Subscription> updateSubscriptions =
        Burst.getStores().getSubscriptionStore().getUpdateSubscriptions(timestamp);
    while(updateSubscriptions.hasNext()) {
      Subscription subscription = updateSubscriptions.next();
      if(removeSubscriptions.contains(subscription.getId())) {
        continue;
      }
      if(subscription.applyUnconfirmed()) {
        appliedSubscriptions.add(subscription);
        totalFees += subscription.getFee();
      }
      else {
        removeSubscriptions.add(subscription.getId());
      }
    }
    return totalFees;
  }

  public static void applyConfirmed(Block block, int blockchainHeight) {
    paymentTransactions.clear();
    for(Subscription subscription : appliedSubscriptions) {
      subscription.apply(block, blockchainHeight);
      subscriptionTable().insert(subscription);
    }
    if(paymentTransactions.size() > 0) {
      Burst.getDbs().getTransactionDb().saveTransactions( paymentTransactions);
    }
    removeSubscriptions.forEach(subscription -> removeSubscription(subscription));
  }

  public final Long senderId;
  public final Long recipientId;
  public final Long id;
  public final BurstKey dbKey;
  public final Long amountNQT;
  public final int frequency;
  private volatile int timeNext;

  private Subscription(Long senderId,
                       Long recipientId,
                       Long id,
                       Long amountNQT,
                       int frequency,
                       int timeStart) {
    this.senderId = senderId;
    this.recipientId = recipientId;
    this.id = id;
    this.dbKey = subscriptionDbKeyFactory().newKey(this.id);
    this.amountNQT = amountNQT;
    this.frequency  = frequency;
    this.timeNext = timeStart + frequency;
  }
  protected Subscription(Long senderId,
                         Long recipientId,
                         Long id,
                         Long amountNQT,
                         int frequency,
                         int timeNext,
                         BurstKey dbKey
                         ) {
    this.senderId = senderId;
    this.recipientId = recipientId;
    this.id = id;
    this.dbKey = dbKey;
    this.amountNQT = amountNQT;
    this.frequency  = frequency;
    this.timeNext = timeNext;
  }

  public Long getSenderId() {
    return senderId;
  }

  public Long getAmountNQT() {
    return amountNQT;
  }

  public Long getRecipientId() {
    return recipientId;
  }

  public Long getId() {
    return id;
  }

  public int getFrequency() {
    return frequency;
  }

  public int getTimeNext() {
    return timeNext;
  }

  private boolean applyUnconfirmed() {
    Account sender = Account.getAccount(senderId);
    long totalAmountNQT = Convert.safeAdd(amountNQT, getFee());

    if(sender.getUnconfirmedBalanceNQT() < totalAmountNQT) {
      return false;
    }

    sender.addToUnconfirmedBalanceNQT(-totalAmountNQT);

    return true;
  }

  private void undoUnconfirmed() {
    Account sender = Account.getAccount(senderId);
    long totalAmountNQT = Convert.safeAdd(amountNQT, getFee());

    sender.addToUnconfirmedBalanceNQT(totalAmountNQT);
  }

  private void apply(Block block, int blockchainHeight) {
    Account sender = Account.getAccount(senderId);
    Account recipient = Account.getAccount(recipientId);

    long totalAmountNQT = Convert.safeAdd(amountNQT, getFee());

    sender.addToBalanceNQT(-totalAmountNQT);
    recipient.addToBalanceAndUnconfirmedBalanceNQT(amountNQT);

    Attachment.AbstractAttachment attachment = new Attachment.AdvancedPaymentSubscriptionPayment(id, blockchainHeight);
    TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl((byte) 1,
                                                                          sender.getPublicKey(), amountNQT,
                                                                          getFee(),
                                                                          timeNext, (short)1440, attachment);

    try {
      builder.senderId(senderId)
          .recipientId(recipientId)
          .blockId(block.getId())
          .height(block.getHeight())
          .blockTimestamp(block.getTimestamp())
          .ecBlockHeight(0)
          .ecBlockId(0L);
      TransactionImpl transaction = builder.build();
      if(!transactionDb.hasTransaction(transaction.getId())) {
        paymentTransactions.add(transaction);
      }
    } catch (NotValidException e) {
      throw new RuntimeException("Failed to build subscription payment transaction", e);
    }

    timeNext += frequency;
  }
}
