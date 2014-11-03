package nxt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nxt.db.Db;
import nxt.db.DbClause;
import nxt.db.DbClause.LongClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nxt.NxtException.NotValidException;
import nxt.util.Convert;

public class Subscription {
	
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
	
	private static final DbKey.LongKeyFactory<Subscription> subscriptionDbKeyFactory = new DbKey.LongKeyFactory<Subscription>("id") {
		@Override
		public DbKey newKey(Subscription subscription) {
			return subscription.dbKey;
		}
	};
	
	private static final VersionedEntityDbTable<Subscription> subscriptionTable = new VersionedEntityDbTable<Subscription>("subscription", subscriptionDbKeyFactory) {
		@Override
		protected Subscription load(Connection con, ResultSet rs) throws SQLException {
			return new Subscription(rs);
		}
		@Override
		protected void save(Connection con, Subscription subscription) throws SQLException {
			subscription.save(con);
		}
		@Override
		protected String defaultSort() {
			return " ORDER BY time_next ASC, id ASC ";
		}
	};
	
	private static final List<TransactionImpl> paymentTransactions = new ArrayList<>();
	private static final List<Subscription> appliedSubscriptions = new ArrayList<>();
	private static final Set<Long> removeSubscriptions = new HashSet<>();
	
	public static long getFee() {
		return Constants.ONE_NXT;
	}
	
	public static DbIterator<Subscription> getAllSubscriptions() {
		return subscriptionTable.getAll(0, -1);
	}
	
	private static DbClause getByParticipantClause(final long id) {
		return new DbClause(" (sender_id = ? OR recipient_id = ?) ") {
			@Override
			public int set(PreparedStatement pstmt, int index) throws SQLException {
				pstmt.setLong(index++, id);
				pstmt.setLong(index++, id);
				return index;
			}
		};
	}
	
	public static DbIterator<Subscription> getSubscriptionsByParticipant(Long accountId) {
		return subscriptionTable.getManyBy(getByParticipantClause(accountId), 0, -1);
	}
	
	public static DbIterator<Subscription> getIdSubscriptions(Long accountId) {
		return subscriptionTable.getManyBy(new DbClause.LongClause("sender_id", accountId), 0, -1);
	}
	
	public static DbIterator<Subscription> getSubscriptionsToId(Long accountId) {
		return subscriptionTable.getManyBy(new DbClause.LongClause("recipient_id", accountId), 0, -1);
	}
	
	public static Subscription getSubscription(Long id) {
		return subscriptionTable.get(subscriptionDbKeyFactory.newKey(id));
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
		
		subscriptionTable.insert(subscription);
	}
	
	public static void removeSubscription(Long id) {
		Subscription subscription = subscriptionTable.get(subscriptionDbKeyFactory.newKey(id));
		if(subscription != null) {
			subscriptionTable.delete(subscription);
		}
	}
	
	private static DbClause getUpdateOnBlockClause(final int timestamp) {
		return new DbClause(" time_next <= ? ") {
			@Override
			public int set(PreparedStatement pstmt, int index) throws SQLException {
				pstmt.setInt(index++, timestamp);
				return index;
			}
		};
	}
	
	@SuppressWarnings("static-access")
	public static long calculateFees(int timestamp) {
		long totalFeeNQT = 0;
		DbIterator<Subscription> updateSubscriptions = subscriptionTable.getManyBy(getUpdateOnBlockClause(timestamp), 0, -1);
		List<Subscription> appliedSubscriptions = new ArrayList<>();
		for(Subscription subscription : updateSubscriptions) {
			if(removeSubscriptions.contains(subscription.getId())) {
				continue;
			}
			if(subscription.applyUnconfirmed()) {
				appliedSubscriptions.add(subscription);
			}
		}
		if(appliedSubscriptions.size() > 0) {
			for(Subscription subscription : appliedSubscriptions) {
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
		DbIterator<Subscription> updateSubscriptions = subscriptionTable.getManyBy(getUpdateOnBlockClause(timestamp), 0, -1);
		for(Subscription subscription : updateSubscriptions) {
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
	
	/*public static void undoUnconfirmed(List<Subscription> subscriptions) {
		for(Subscription subscription : subscriptions) {
			subscription.undoUnconfirmed();
		}
	}*/
	
	public static void applyConfirmed(Block block) {
		paymentTransactions.clear();
		for(Subscription subscription : appliedSubscriptions) {
			subscription.apply(block);
			subscriptionTable.insert(subscription);
		}
		if(paymentTransactions.size() > 0) {
			try (Connection con = Db.getConnection()) {
				TransactionDb.saveTransactions(con, paymentTransactions);
			}
			catch(SQLException e) {
				throw new RuntimeException(e.toString(), e);
			}
		}
		for(Long subscription : removeSubscriptions) {
			removeSubscription(subscription);
		}
	}
	
	private final Long senderId;
	private final Long recipientId;
	private final Long id;
	private final DbKey dbKey;
	private final Long amountNQT;
	private final int frequency;
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
		this.dbKey = subscriptionDbKeyFactory.newKey(this.id);
		this.amountNQT = amountNQT;
		this.frequency  = frequency;
		this.timeNext = timeStart + frequency;
	}
	
	private Subscription(ResultSet rs) throws SQLException {
		this.id = rs.getLong("id");
		this.dbKey = subscriptionDbKeyFactory.newKey(this.id);
		this.senderId = rs.getLong("sender_id");
		this.recipientId = rs.getLong("recipient_id");
		this.amountNQT = rs.getLong("amount");
		this.frequency = rs.getInt("frequency");
		this.timeNext = rs.getInt("time_next");
	}
	
	private void save(Connection con) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO subscription (id, "
				+ "sender_id, recipient_id, amount, frequency, time_next, height, latest) "
				+ "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
			int i = 0;
			pstmt.setLong(++i, this.id);
			pstmt.setLong(++i, this.senderId);
			pstmt.setLong(++i, this.recipientId);
			pstmt.setLong(++i, this.amountNQT);
			pstmt.setInt(++i, this.frequency);
			pstmt.setInt(++i, this.timeNext);
			pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
			pstmt.executeUpdate();
		}
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
	
	private void apply(Block block) {
		Account sender = Account.getAccount(senderId);
		Account recipient = Account.getAccount(recipientId);
		
		long totalAmountNQT = Convert.safeAdd(amountNQT, getFee());
		
		sender.addToBalanceNQT(-totalAmountNQT);
		recipient.addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
		
		Attachment.AbstractAttachment attachment = new Attachment.AdvancedPaymentSubscriptionPayment(id);
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
			if(!TransactionDb.hasTransaction(transaction.getId())) {
				paymentTransactions.add(transaction);
			}
		} catch (NotValidException e) {
			throw new RuntimeException("Failed to build subscription payment transaction");
		}
		
		timeNext += frequency;
	}
}
