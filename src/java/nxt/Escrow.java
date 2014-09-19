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
	
	public static enum Decision {
		UNDECIDED,
		RELEASE,
		REFUND,
		SPLIT;
	}
	
	public static String decisionToString(Decision decision) {
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
	
	public static Decision stringToDecision(String decision) {
		switch(decision) {
		case "undecided":
			return Decision.UNDECIDED;
		case "release":
			return Decision.RELEASE;
		case "refund":
			return Decision.REFUND;
		case "split":
			return Decision.SPLIT;
		}
		
		return null;
	}
	
	public static Byte decisionToByte(Decision decision) {
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
	
	public static Decision byteToDecision(Byte decision) {
		switch(decision) {
		case 0:
			return Decision.UNDECIDED;
		case 1:
			return Decision.RELEASE;
		case 2:
			return Decision.REFUND;
		case 3:
			return Decision.SPLIT;
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
											Collection<Long> signers,
											int deadline,
											Decision deadlineAction) {
		Escrow newEscrowTransaction = new Escrow(sender,
												 recipient,
												 id,
												 amountNQT,
												 requiredSigners,
												 signers,
												 deadline,
												 deadlineAction);
		
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
	private final ConcurrentMap<Long, Decision> decisions = new ConcurrentHashMap<>();
	private final int deadline;
	private final Decision deadlineAction;
	private volatile boolean release = false;
	private volatile boolean refund = false;
	private volatile boolean complete = false;
	
	private Escrow(Account sender,
				   Account recipient,
				   Long id,
				   Long amountNQT,
				   int requiredSigners,
				   Collection<Long> signers,
				   int deadline,
				   Decision deadlineAction) {
		this.senderId = sender.getId();
		this.recipientId = recipient.getId();
		this.id = id;
		this.amountNQT = amountNQT;
		this.requiredSigners = requiredSigners;
		for(Long signer : signers) {
			decisions.put(signer, Decision.UNDECIDED);
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
		return Collections.unmodifiableCollection(decisions.keySet());
	}
	
	public int getDeadline() {
		return deadline;
	}
	
	public boolean isIdSigner(Long id) {
		return decisions.containsKey(id);
	}
	
	public Decision getIdDecision(Long id) {
		return decisions.get(id);
	}
	
	public int getDecisionCount(Decision decision) {
		int count = 0;
		for(Decision d : decisions.values()) {
			if(d == decision) {
				count++;
			}
		}
		return count;
	}
	
	public synchronized void sign(Long id, Decision decision) {
		if(id.equals(senderId)) {
			if(decision == Decision.RELEASE) {
				release = true;
				complete = true;
			}
			return;
		}
		
		if(id.equals(recipientId)) {
			if(decision == Decision.REFUND) {
				refund = true;
				complete = true;
			}
			return;
		}
		
		if(complete) {
			return;
		}
		
		if(!decisions.containsKey(id)) {
			return;
		}
		
		decisions.put(id, decision);
		
		checkComplete();
	}
	
	public synchronized boolean isComplete() {
		return complete;
	}
	
	private void checkComplete() {
		if(complete || release || refund) {
			complete = true;
			return;
		}
		
		if(getDecisionCount(Decision.RELEASE) >= requiredSigners ||
		   getDecisionCount(Decision.REFUND) >= requiredSigners ||
		   getDecisionCount(Decision.SPLIT) >= requiredSigners) {
			complete = true;
			return;
		}
		
		complete = false;
		return;
	}
	
	private synchronized Decision doPayout() {
		Decision result = deadlineAction; // default to deadline up if no other result
		
		if(release) {
			result = Decision.RELEASE;
		}
		else if(refund) {
			result = Decision.REFUND;
		}
		else if(getDecisionCount(Decision.RELEASE) >= requiredSigners) {
			result = Decision.RELEASE;
		}
		else if(getDecisionCount(Decision.REFUND) >= requiredSigners) {
			result = Decision.REFUND;
		}
		else if(getDecisionCount(Decision.SPLIT) >= requiredSigners) {
			result = Decision.SPLIT;
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
