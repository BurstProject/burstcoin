package nxt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nxt.util.Convert;

public class Escrow {
	
	public static enum Vote {
		UNDECIDED,
		RELEASE,
		REFUND,
		SPLIT;
	}
	
	public static String voteToString(Vote vote) {
		switch(vote) {
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
	
	public static Vote stringToVote(String vote) {
		switch(vote) {
		case "undecided":
			return Vote.UNDECIDED;
		case "release":
			return Vote.RELEASE;
		case "refund":
			return Vote.REFUND;
		case "split":
			return Vote.SPLIT;
		}
		
		return null;
	}
	
	private static final ConcurrentMap<Long, Escrow> escrowTransactions = new ConcurrentHashMap<>();
	private static final Collection<Escrow> allEscrowTransactions = Collections.unmodifiableCollection(escrowTransactions.values());
	
	private static Long lastUnpoppableBlock = 0L;
	
	public static Collection<Escrow> getAllEscrowTransactions() {
		return allEscrowTransactions;
	}
	
	public static Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId) {
		List<Escrow> filtered = new ArrayList<>();
		for(Escrow escrow : Escrow.getAllEscrowTransactions()) {
			if(escrow.getSenderId() == accountId ||
			   escrow.getRecipientId() == accountId ||
			   escrow.isIdSigner(accountId)) {
				filtered.add(escrow);
			}
		}
		return filtered;
	}
	
	public static Escrow getEscrowTransaction(Long id) {
		return escrowTransactions.get(id);
	}
	
	public static void addEscrowTransaction(Account sender,
											Account recipient,
											Long id,
											Long amountNQT,
											int requiredSigners,
											List<Long> signers,
											int deadline,
											String deadlineAction) {
		Escrow newEscrowTransaction = new Escrow(sender,
												 recipient,
												 id,
												 amountNQT,
												 requiredSigners,
												 signers,
												 deadline,
												 stringToVote(deadlineAction));
		
		escrowTransactions.put(id, newEscrowTransaction);
	}
	
	public static void removeEscrowTransaction(Long id) {
		escrowTransactions.remove(id);
	}
	
	public static void updateOnBlock(Long blockId, int timestamp) {
		Iterator<Entry<Long, Escrow>> it = escrowTransactions.entrySet().iterator();
		while(it.hasNext()) {
			Entry<Long, Escrow> escrow = it.next();
			if(escrow.getValue().isComplete() ||
			   escrow.getValue().getDeadline() < timestamp) {
				escrow.getValue().doPayout();
				if(escrow.getValue().getDeadline() < timestamp) {
					lastUnpoppableBlock = blockId;
				}
				it.remove();
			}
		}
	}
	
	public static Long getUnpoppableBlockId() {
		return lastUnpoppableBlock;
	}
	
	public static void clear() {
		escrowTransactions.clear();
		lastUnpoppableBlock = 0L;
	}
	
	private final Long senderId;
	private final Long recipientId;
	private final Long id;
	private final Long amountNQT;
	private final int requiredSigners;
	private final ConcurrentMap<Long, Vote> votes = new ConcurrentHashMap<>();
	private final int deadline;
	private final Vote deadlineAction;
	private volatile boolean release = false;
	private volatile boolean refund = false;
	private volatile boolean complete = false;
	
	private Escrow(Account sender,
				   Account recipient,
				   Long id,
				   Long amountNQT,
				   int requiredSigners,
				   List<Long> signers,
				   int deadline,
				   Vote deadlineAction) {
		this.senderId = sender.getId();
		this.recipientId = recipient.getId();
		this.id = id;
		this.amountNQT = amountNQT;
		this.requiredSigners = requiredSigners;
		for(Long signer : signers) {
			votes.put(signer, Vote.UNDECIDED);
		}
		this.deadline = deadline;
		this.deadlineAction = deadlineAction;
	}
	
	public Long getSenderId() {
		return senderId;
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
	
	public Collection<Long> getSigners() {
		return Collections.unmodifiableCollection(votes.keySet());
	}
	
	public int getDeadline() {
		return deadline;
	}
	
	public boolean isIdSigner(Long id) {
		return votes.containsKey(id);
	}
	
	public Vote getIdVote(Long id) {
		return votes.get(id);
	}
	
	public int getVotesUndecided() {
		int undecided = 0;
		for(Vote vote : votes.values()) {
			if(vote == Vote.UNDECIDED) {
				undecided++;
			}
		}
		return undecided;
	}
	
	public int getVotesRelease() {
		int release = 0;
		for(Vote vote : votes.values()) {
			if(vote == Vote.RELEASE) {
				release++;
			}
		}
		return release;
	}
	
	public int getVotesRefund() {
		int refund = 0;
		for(Vote vote : votes.values()) {
			if(vote == Vote.REFUND) {
				refund++;
			}
		}
		return refund;
	}
	
	public int getVotesSplit() {
		int split = 0;
		for(Vote vote : votes.values()) {
			if(vote == Vote.SPLIT) {
				split++;
			}
		}
		return split;
	}
	
	public synchronized void vote(Long id, Vote vote) {
		if(complete) {
			return;
		}
		
		if(!votes.containsKey(id)) {
			return;
		}
		
		votes.put(id, vote);
		
		checkFinished();
	}
	
	public synchronized void setRelease() {
		release = true;
		complete = true;
	}
	
	public synchronized void setRefund() {
		refund = true;
		complete = true;
	}
	
	public synchronized boolean isComplete() {
		return complete;
	}
	
	private void checkFinished() {
		if(complete || release || refund) {
			complete = true;
			return;
		}
		int votesRelease = 0;
		int votesRefund = 0;
		int votesSplit = 0;
		for(Vote vote : votes.values()) {
			switch(vote) {
			case RELEASE:
				votesRelease++;
				break;
			case REFUND:
				votesRefund++;
				break;
			case SPLIT:
				votesSplit++;
				break;
			default:
				break;
			}
		}
		if(votesRelease >= requiredSigners ||
		   votesRefund >= requiredSigners ||
		   votesSplit >= requiredSigners) {
			complete = true;
			return;
		}
		complete = false;
		return;
	}
	
	private synchronized Vote doPayout() {
		Vote result = deadlineAction; // default to deadline up if no other result
		
		if(release) {
			result = Vote.RELEASE;
		}
		else if(refund) {
			result = Vote.REFUND;
		}
		else if(getVotesRelease() >= requiredSigners) {
			result = Vote.RELEASE;
		}
		else if(getVotesRefund() >= requiredSigners) {
			result = Vote.REFUND;
		}
		else if(getVotesSplit() >= requiredSigners) {
			result = Vote.SPLIT;
		}
		
		switch(result) {
		case RELEASE:
			Account.getAccount(recipientId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
			break;
		case REFUND:
			Account.getAccount(senderId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
			break;
		case SPLIT:
			Long halfAmountNQT = amountNQT / 2;
			Account.getAccount(recipientId).addToBalanceAndUnconfirmedBalanceNQT(halfAmountNQT);
			Account.getAccount(senderId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT - halfAmountNQT);
			break;
		default: // should never get here
			break;
		}
		
		return result;
	}

}
