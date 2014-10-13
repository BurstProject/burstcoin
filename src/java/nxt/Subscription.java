package nxt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import nxt.NxtException.NotValidException;
import nxt.util.Convert;

public class Subscription implements Comparable<Subscription> {
	
	public static boolean isEnabled() {
		if(Nxt.getBlockchain().getLastBlock().getHeight() >= Constants.BURST_SUBSCRIPTION_START_BLOCK) {
			return true;
		}
		
		Alias subscriptionEnabled = Alias.getAlias("featuresubscription");
		if(subscriptionEnabled != null && subscriptionEnabled.getAliasURI().equals("enabled")) {
			return true;
		}
		
		return false;
	}
	
	private static final ConcurrentMap<Long, Subscription> subscriptions = new ConcurrentHashMap<>();
	private static final ConcurrentSkipListSet<Subscription> subscriptionsSorted = new ConcurrentSkipListSet<>();
	private static final Collection<Subscription> allSubscriptions = Collections.unmodifiableCollection(subscriptionsSorted);

	private volatile static Long lastUnpoppableBlock = 0L;
	private static final Subscription cutoffSubscription = new Subscription(null, null, Long.MAX_VALUE, 0L, 0, 0);
	
	private static final List<TransactionImpl> paymentTransactions = new ArrayList<>();
	
	public static Collection<Subscription> getAllSubscriptions() {
		return allSubscriptions;
	}
	
	public static Collection<Subscription> getSubscriptionsByParticipent(Long accountId) {
		List<Subscription> filtered = new ArrayList<>();
		for(Subscription subscription : Subscription.getAllSubscriptions()) {
			if(subscription.getSenderId().equals(accountId) ||
			   subscription.getRecipientId().equals(accountId)) {
				filtered.add(subscription);
			}
		}
		return filtered;
	}
	
	public static Collection<Subscription> getIdSubscriptions(Long accountId) {
		List<Subscription> filtered = new ArrayList<>();
		for(Subscription subscription : Subscription.getAllSubscriptions()) {
			if(subscription.getSenderId().equals(accountId)) {
				filtered.add(subscription);
			}
		}
		return filtered;
	}
	
	public static Collection<Subscription> getSubscriptionsToId(Long accountId) {
		List<Subscription> filtered = new ArrayList<>();
		for(Subscription subscription : Subscription.getAllSubscriptions()) {
			if(subscription.getRecipientId().equals(accountId)) {
				filtered.add(subscription);
			}
		}
		return filtered;
	}
	
	public static Subscription getSubscription(Long id) {
		return subscriptions.get(id);
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
		
		subscriptions.put(subscription.getId(), subscription);
		subscriptionsSorted.add(subscription);
	}
	
	public static void removeSubscription(Long id) {
		Subscription subscription = subscriptions.get(id);
		subscriptionsSorted.remove(subscription);
		subscriptions.remove(id);
	}
	
	public static long updateOnBlock(Long blockId, int blockHeight, int timestamp, boolean apply, boolean scanning) throws NotValidException {
		long totalFeeNQT = 0;
		
		paymentTransactions.clear();
		
		List<Subscription> retainSubscriptions = new ArrayList<>();
		cutoffSubscription.timeNext = timestamp;
		NavigableSet<Subscription> processingSubscriptions = subscriptionsSorted.headSet(cutoffSubscription, true);
		Iterator<Subscription> it = processingSubscriptions.iterator();
		while(it.hasNext()) {
			Subscription subscription = it.next();
			boolean retain = subscription.process(blockId, blockHeight, timestamp, apply, scanning);
			if(retain) {
				retainSubscriptions.add(subscription);
			}
			else if(apply) {
				subscriptions.remove(subscription.getId());
				lastUnpoppableBlock = blockId;
			}
			if(apply) {
				it.remove();
			}
		}
		
		if(retainSubscriptions.size() > 0) {
			totalFeeNQT = Convert.safeMultiply(retainSubscriptions.size(), Convert.safeDivide(Constants.ONE_NXT, 10L));
			if(apply) {
				subscriptionsSorted.addAll(retainSubscriptions);
			}
		}
		
		return totalFeeNQT;
	}
	
	public static void saveTransactions() {
		if(paymentTransactions.size() > 0) {
			try (Connection con = Db.getConnection()) {
				TransactionDb.saveTransactions(con, paymentTransactions);
			}
			catch(SQLException e) {
				throw new RuntimeException(e.toString(), e);
			}
			paymentTransactions.clear();
		}
	}
	
	public static void undoBlock(int prevBlockTimestamp) {
		Iterator<Subscription> it = subscriptionsSorted.iterator();
		while(it.hasNext()) {
			Subscription subscription = it.next();
			subscription.undo(prevBlockTimestamp);
		}
		subscriptionsSorted.clear();
		subscriptionsSorted.addAll(subscriptions.values());
	}
	
	public static Long getUnpoppableBlockId() {
		return lastUnpoppableBlock;
	}
	
	public static void clear() {
		subscriptions.clear();
		subscriptionsSorted.clear();
		lastUnpoppableBlock = 0L;
	}
	
	private final Long senderId;
	private final long recipientId;
	private final Long id;
	private final Long amountNQT;
	private final int frequency;
	private final int timeStart;
	private volatile int timeNext;
	private volatile int timeLast;
	
	private Subscription(Long senderId,
						 Long recipientId,
						 Long id,
						 Long amountNQT,
						 int frequency,
						 int timeStart) {
		this.senderId = senderId;
		this.recipientId = recipientId;
		this.id = id;
		this.amountNQT = amountNQT;
		this.frequency  = frequency;
		this.timeStart = timeStart;
		this.timeLast = 0;
		this.timeNext = timeStart + frequency;
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
	
	private synchronized boolean process(Long blockId, int blockHeight, int blockTime, boolean apply, boolean scanning) throws NotValidException {
		if(blockTime < timeNext) {
			return true;
		}
		
		Account sender = Account.getAccount(senderId);
		Account recipient = Account.getAccount(recipientId);
		
		long totalAmountNQT = Convert.safeAdd(amountNQT, Convert.safeDivide(Constants.ONE_NXT, 10L));
		
		if(sender.getBalanceNQT() < totalAmountNQT) {
			return false;
		}
		
		if(apply) {
			sender.addToBalanceAndUnconfirmedBalanceNQT(-totalAmountNQT);
			recipient.addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
			
			timeLast = timeNext;
			timeNext += frequency;
			
			if(!scanning) {
				Attachment.AbstractAttachment attachment = new Attachment.AdvancedPaymentSubscriptionPayment(id);
				TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl((byte) 1,
																			  sender.getPublicKey(), amountNQT,
																			  Convert.safeDivide(Constants.ONE_NXT, 10L),
																			  timeLast, (short)1440, attachment);
				builder.referencedTransactionFullHash((String)null)
					   .signature(null)
					   .blockId(blockId)
					   .height(blockHeight)
					   .id(null)
					   .senderId(null)
					   .blockTimestamp(blockTime)
					   .fullHash((String)null)
					   .ecBlockHeight(0)
					   .ecBlockId(null);
				TransactionImpl transaction = builder.build();
				paymentTransactions.add(transaction);
			}
		}
		
		return true;
	}
	
	private synchronized void undo(int prevBlockTime) {
		if(prevBlockTime >= timeLast) {
			return;
		}
		
		Account sender = Account.getAccount(senderId);
		Account recipient = Account.getAccount(recipientId);
		
		recipient.addToBalanceAndUnconfirmedBalanceNQT(-amountNQT);
		sender.addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
		
		timeNext = timeLast;
		timeLast -= frequency;
		
		if(timeLast <= timeStart) {
			timeLast = 0;
		}
	}

	@Override
	public int compareTo(Subscription o) {
		if(timeNext < o.timeNext) {
			return -1;
		}
		
		if(timeNext > o.timeNext) {
			return 1;
		}
		
		return id.compareTo(o.id);
	}
}
