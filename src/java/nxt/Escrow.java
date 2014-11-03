package nxt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

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

import nxt.util.Convert;

public class Escrow {
	
	public static enum DecisionType {
		UNDECIDED,
		RELEASE,
		REFUND,
		SPLIT;
	}
	
	public static String decisionToString(DecisionType decision) {
		switch(decision) {
		case UNDECIDED:
			return "undecided";
		case RELEASE:
			return "release";
		case REFUND:
			return "refund";
		case SPLIT:
			return "split";
		}
		
		return null;
	}
	
	public static DecisionType stringToDecision(String decision) {
		switch(decision) {
		case "undecided":
			return DecisionType.UNDECIDED;
		case "release":
			return DecisionType.RELEASE;
		case "refund":
			return DecisionType.REFUND;
		case "split":
			return DecisionType.SPLIT;
		}
		
		return null;
	}
	
	public static Byte decisionToByte(DecisionType decision) {
		switch(decision) {
		case UNDECIDED:
			return 0;
		case RELEASE:
			return 1;
		case REFUND:
			return 2;
		case SPLIT:
			return 3;
		}
		
		return null;
	}
	
	public static DecisionType byteToDecision(Byte decision) {
		switch(decision) {
		case 0:
			return DecisionType.UNDECIDED;
		case 1:
			return DecisionType.RELEASE;
		case 2:
			return DecisionType.REFUND;
		case 3:
			return DecisionType.SPLIT;
		}
		
		return null;
	}
	
	public static boolean isEnabled() {
		if(Nxt.getBlockchain().getLastBlock().getHeight() >= Constants.BURST_ESCROW_START_BLOCK) {
			return true;
		}
		
		Alias escrowEnabled = Alias.getAlias("featureescrow");
		if(escrowEnabled != null && escrowEnabled.getAliasURI().equals("enabled")) {
			return true;
		}
		return false;
	}
	
	public static class Decision {
		
		private final Long escrowId;
		private final Long accountId;
		private final DbKey dbKey;
		private DecisionType decision;
		
		private Decision(Long escrowId, Long accountId, DecisionType decision) {
			this.escrowId = escrowId;
			this.accountId = accountId;
			this.dbKey = decisionDbKeyFactory.newKey(this.escrowId, this.accountId);
			this.decision = decision;
		}
		
		private Decision(ResultSet rs) throws SQLException {
			this.escrowId = rs.getLong("escrow_id");
			this.accountId = rs.getLong("account_id");
			this.dbKey = decisionDbKeyFactory.newKey(this.escrowId, this.accountId);
			this.decision = byteToDecision((byte)rs.getInt("decision"));
		}
		
		private void save(Connection con) throws SQLException {
			try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO escrow_decision (escrow_id, "
					+ "account_id, decision, height, latest) KEY (escrow_id, account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
				int i = 0;
				pstmt.setLong(++i, this.escrowId);
				pstmt.setLong(++i, this.accountId);
				pstmt.setInt(++i, decisionToByte(this.decision));
				pstmt.setInt(++i,  Nxt.getBlockchain().getHeight());
				pstmt.executeUpdate();
			}
		}
		
		public Long getEscrowId() {
			return this.escrowId;
		}
		
		public Long getAccountId() {
			return this.accountId;
		}
		
		public DecisionType getDecision() {
			return this.decision;
		}
		
		public void setDecision(DecisionType decision) {
			this.decision = decision;
		}
	}
	
	private static final DbKey.LongKeyFactory<Escrow> escrowDbKeyFactory = new DbKey.LongKeyFactory<Escrow>("id") {
		@Override
		public DbKey newKey(Escrow escrow) {
			return escrow.dbKey;
		}
	};
	
	private static final VersionedEntityDbTable<Escrow> escrowTable = new VersionedEntityDbTable<Escrow>("escrow", escrowDbKeyFactory) {
		@Override
		protected Escrow load(Connection con, ResultSet rs) throws SQLException {
			return new Escrow(rs);
		}
		@Override
		protected void save(Connection con, Escrow escrow) throws SQLException {
			escrow.save(con);
		}
	};
	
	private static final DbKey.LinkKeyFactory<Decision> decisionDbKeyFactory = new DbKey.LinkKeyFactory<Decision>("escrow_id", "account_id") {
		@Override
		public DbKey newKey(Decision decision) {
			return decision.dbKey;
		}
	};
	
	private static final VersionedEntityDbTable<Decision> decisionTable = new VersionedEntityDbTable<Decision>("escrow_decision", decisionDbKeyFactory) {
		@Override
		protected Decision load(Connection con, ResultSet rs) throws SQLException {
			return new Decision(rs);
		}
		@Override
		protected void save(Connection con, Decision decision) throws SQLException {
			decision.save(con);
		}
	};
	
	private static final ConcurrentSkipListSet<Long> updatedEscrowIds = new ConcurrentSkipListSet<>();
	private static final List<TransactionImpl> resultTransactions = new ArrayList<>();
	
	public static DbIterator<Escrow> getAllEscrowTransactions() {
		return escrowTable.getAll(0, -1);
	}
	
	private static DbClause getEscrowParticipentClause(final long accountId) {
		return new DbClause(" (sender_id = ? OR recipient_id = ?) ") {
			@Override
	        public int set(PreparedStatement pstmt, int index) throws SQLException {
	            pstmt.setLong(index++, accountId);
	            pstmt.setLong(index++, accountId);
	            return index;
	        }
		};
	}
	
	public static Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId) {
		List<Escrow> filtered = new ArrayList<>();
		DbIterator<Decision> it = decisionTable.getManyBy(new DbClause.LongClause("account_id", accountId), 0, -1);
		while(it.hasNext()) {
			Decision decision = it.next();
			Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(decision.escrowId));
			if(escrow != null) {
				filtered.add(escrow);
			}
		}
		return filtered;
	}
	
	public static Escrow getEscrowTransaction(Long id) {
		return escrowTable.get(escrowDbKeyFactory.newKey(id));
	}
	
	public static void addEscrowTransaction(Account sender,
											Account recipient,
											Long id,
											Long amountNQT,
											int requiredSigners,
											Collection<Long> signers,
											int deadline,
											DecisionType deadlineAction) {
		Escrow newEscrowTransaction = new Escrow(sender,
												 recipient,
												 id,
												 amountNQT,
												 requiredSigners,
												 deadline,
												 deadlineAction);
		escrowTable.insert(newEscrowTransaction);
		
		Decision senderDecision = new Decision(id, sender.getId(), DecisionType.UNDECIDED);
		decisionTable.insert(senderDecision);
		Decision recipientDecision = new Decision(id, recipient.getId(), DecisionType.UNDECIDED);
		decisionTable.insert(recipientDecision);
		for(Long signer : signers) {
			Decision decision = new Decision(id, signer, DecisionType.UNDECIDED);
			decisionTable.insert(decision);
		}
	}
	
	public static void removeEscrowTransaction(Long id) {
		Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(id));
		if(escrow == null) {
			return;
		}
		DbIterator<Decision> decisionIt = escrow.getDecisions();
		
		List<Decision> decisions = new ArrayList<>();
		while(decisionIt.hasNext()) {
			Decision decision = decisionIt.next();
			decisions.add(decision);
		}
		
		for(Decision decision : decisions) {
			decisionTable.delete(decision);
		}
		escrowTable.delete(escrow);
	}
	
	private static DbClause getUpdateOnBlockClause(final int timestamp) {
		return new DbClause(" deadline < ? ") {
			@Override
			public int set(PreparedStatement pstmt, int index) throws SQLException {
				pstmt.setInt(index++, timestamp);
				return index;
			}
		};
	}
	
	public static void updateOnBlock(Block block) {
		resultTransactions.clear();
		
		DbIterator<Escrow> deadlineEscrows = escrowTable.getManyBy(getUpdateOnBlockClause(block.getTimestamp()), 0, -1);
		for(Escrow escrow : deadlineEscrows) {
			updatedEscrowIds.add(escrow.getId());
		}
		
		if(updatedEscrowIds.size() > 0) {
			for(Long escrowId : updatedEscrowIds) {
				Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(escrowId));
				DecisionType result = escrow.checkComplete();
				if(result != DecisionType.UNDECIDED || escrow.getDeadline() < block.getTimestamp()) {
					if(result == DecisionType.UNDECIDED) {
						result = escrow.getDeadlineAction();
					}
					escrow.doPayout(result, block);
					
					removeEscrowTransaction(escrowId);
				}
			}
			if(resultTransactions.size() > 0) {
				try (Connection con = Db.getConnection()) {
					TransactionDb.saveTransactions(con, resultTransactions);
				}
				catch(SQLException e) {
					throw new RuntimeException(e.toString(), e);
				}
			}
			updatedEscrowIds.clear();
		}
	}
	
	private final Long senderId;
	private final Long recipientId;
	private final Long id;
	private final DbKey dbKey;
	private final Long amountNQT;
	private final int requiredSigners;
	private final int deadline;
	private final DecisionType deadlineAction;
	
	private Escrow(Account sender,
				   Account recipient,
				   Long id,
				   Long amountNQT,
				   int requiredSigners,
				   int deadline,
				   DecisionType deadlineAction) {
		this.senderId = sender.getId();
		this.recipientId = recipient.getId();
		this.id = id;
		this.dbKey = escrowDbKeyFactory.newKey(this.id);
		this.amountNQT = amountNQT;
		this.requiredSigners = requiredSigners;
		this.deadline = deadline;
		this.deadlineAction = deadlineAction;
	}
	
	private Escrow(ResultSet rs) throws SQLException {
		this.id = rs.getLong("id");
		this.dbKey = escrowDbKeyFactory.newKey(this.id);
		this.senderId = rs.getLong("sender_id");
		this.recipientId = rs.getLong("recipient_id");
		this.amountNQT = rs.getLong("amount");
		this.requiredSigners = rs.getInt("required_signers");
		this.deadline = rs.getInt("deadline");
		this.deadlineAction = byteToDecision((byte)rs.getInt("deadline_action"));
	}
	
	private void save(Connection con) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO escrow (id, sender_id, "
				+ "recipient_id, amount, required_signers, deadline, deadline_action, height, latest) "
				+ "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
			int i = 0;
			pstmt.setLong(++i, this.id);
			pstmt.setLong(++i, this.senderId);
			pstmt.setLong(++i, this.recipientId);
			pstmt.setLong(++i, this.amountNQT);
			pstmt.setInt(++i, this.requiredSigners);
			pstmt.setInt(++i, this.deadline);
			pstmt.setInt(++i, decisionToByte(this.deadlineAction));
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
	
	public int getRequiredSigners() {
		return requiredSigners;
	}
	
	public DbIterator<Decision> getDecisions() {
		return decisionTable.getManyBy(new DbClause.LongClause("escrow_id", id), 0, -1);
	}
	
	public int getDeadline() {
		return deadline;
	}
	
	public DecisionType getDeadlineAction() {
		return deadlineAction;
	}
	
	public boolean isIdSigner(Long id) {
		return decisionTable.get(decisionDbKeyFactory.newKey(this.id, id)) != null;
	}
	
	public Decision getIdDecision(Long id) {
		return decisionTable.get(decisionDbKeyFactory.newKey(this.id, id));
	}
	
	public synchronized void sign(Long id, DecisionType decision) {
		if(id.equals(senderId) && decision != DecisionType.RELEASE) {
			return;
		}
		
		if(id.equals(recipientId) && decision != DecisionType.REFUND) {
			return;
		}
		
		Decision decisionChange = decisionTable.get(decisionDbKeyFactory.newKey(this.id, id));
		if(decisionChange == null) {
			return;
		}
		decisionChange.setDecision(decision);
		
		decisionTable.insert(decisionChange);
		
		if(!updatedEscrowIds.contains(this.id)) {
			updatedEscrowIds.add(this.id);
		}
	}
	
	private DecisionType checkComplete() {
		Decision senderDecision = decisionTable.get(decisionDbKeyFactory.newKey(id, senderId));
		if(senderDecision.getDecision() == DecisionType.RELEASE) {
			return DecisionType.RELEASE;
		}
		Decision recipientDecision = decisionTable.get(decisionDbKeyFactory.newKey(id, recipientId));
		if(recipientDecision.getDecision() == DecisionType.REFUND) {
			return DecisionType.REFUND;
		}
		
		int countRelease = 0;
		int countRefund = 0;
		int countSplit = 0;
		
		DbIterator<Decision> decisions = decisionTable.getManyBy(new DbClause.LongClause("escrow_id", id), 0, -1);
		while(decisions.hasNext()) {
			Decision decision = decisions.next();
			if(decision.getAccountId().equals(senderId) ||
			   decision.getAccountId().equals(recipientId)) {
				continue;
			}
			switch(decision.getDecision()) {
			case RELEASE:
				countRelease++;
				break;
			case REFUND:
				countRefund++;
				break;
			case SPLIT:
				countSplit++;
				break;
			default:
				break;
			}
		}
		
		if(countRelease >= requiredSigners) {
			return DecisionType.RELEASE;
		}
		if(countRefund >= requiredSigners) {
			return DecisionType.REFUND;
		}
		if(countSplit >= requiredSigners) {
			return DecisionType.SPLIT;
		}
		
		return DecisionType.UNDECIDED;
	}
	
	private synchronized void doPayout(DecisionType result, Block block) {
		switch(result) {
		case RELEASE:
			Account.getAccount(recipientId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
			saveResultTransaction(block, id, recipientId, amountNQT, DecisionType.RELEASE);
			break;
		case REFUND:
			Account.getAccount(senderId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
			saveResultTransaction(block, id, senderId, amountNQT, DecisionType.REFUND);
			break;
		case SPLIT:
			Long halfAmountNQT = amountNQT / 2;
			Account.getAccount(recipientId).addToBalanceAndUnconfirmedBalanceNQT(halfAmountNQT);
			Account.getAccount(senderId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT - halfAmountNQT);
			saveResultTransaction(block, id, recipientId, halfAmountNQT, DecisionType.SPLIT);
			saveResultTransaction(block, id, senderId, amountNQT - halfAmountNQT, DecisionType.SPLIT);
			break;
		default: // should never get here
			break;
		}
	}
	
	private static void saveResultTransaction(Block block, Long escrowId, Long recipientId, Long amountNQT, DecisionType decision) {
		Attachment.AbstractAttachment attachment = new Attachment.AdvancedPaymentEscrowResult(escrowId, decision);
		TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl((byte)1, Genesis.CREATOR_PUBLIC_KEY,
																	  amountNQT, 0L, block.getTimestamp(), (short)1440, attachment);
		builder.senderId(0L)
		   .recipientId(recipientId)
		   .blockId(block.getId())
		   .height(block.getHeight())
		   .blockTimestamp(block.getTimestamp())
		   .ecBlockHeight(0)
		   .ecBlockId(0L);
		
		TransactionImpl transaction = null;
		try {
			transaction = builder.build();
		}
		catch(NxtException.NotValidException e) {
			throw new RuntimeException(e.toString(), e);
		}
		
		if(!TransactionDb.hasTransaction(transaction.getId())) {
			resultTransactions.add(transaction);
		}
	}
}
