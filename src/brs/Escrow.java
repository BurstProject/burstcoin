package brs;

import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class Escrow {

  public enum DecisionType {
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
    if(Burst.getBlockchain().getLastBlock().getHeight() >= Constants.BURST_ESCROW_START_BLOCK) {
      return true;
    }

    Alias escrowEnabled = Alias.getAlias("featureescrow");
    if(escrowEnabled != null && escrowEnabled.getAliasURI().equals("enabled")) {
      return true;
    }
    return false;
  }

  public static class Decision {

    public final Long escrowId;
    public final Long accountId;
    public final BurstKey dbKey;
    private DecisionType decision;

    protected Decision(Long escrowId, Long accountId, DecisionType decision) {
      this.escrowId = escrowId;
      this.accountId = accountId;
      this.dbKey = decisionDbKeyFactory().newKey(this.escrowId, this.accountId);
      this.decision = decision;
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

  private static final BurstKey.LongKeyFactory<Escrow> escrowDbKeyFactory() {
    return Burst.getStores().getEscrowStore().getEscrowDbKeyFactory();
  }

  private static final VersionedEntityTable<Escrow> escrowTable() {
    return Burst.getStores().getEscrowStore().getEscrowTable();
  }

  private static final BurstKey.LinkKeyFactory<Decision> decisionDbKeyFactory() {
    return Burst.getStores().getEscrowStore().getDecisionDbKeyFactory();
  }

  private static final VersionedEntityTable<Decision> decisionTable() {
    return Burst.getStores().getEscrowStore().getDecisionTable();
  }

  /** WATCH: Thread-Safety?! */
  private static final ConcurrentSkipListSet<Long> updatedEscrowIds() {
    return Burst.getStores().getEscrowStore().getUpdatedEscrowIds();
  }

  /** WATCH: Thread-Safety?! */
  private static final List<TransactionImpl> resultTransactions() {
    return Burst.getStores().getEscrowStore().getResultTransactions();
  }

  public static Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId) {
    return Burst.getStores().getEscrowStore().getEscrowTransactionsByParticipent(accountId);
  }

  public static Escrow getEscrowTransaction(Long id) {
    return escrowTable().get(escrowDbKeyFactory().newKey(id));
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
    escrowTable().insert(newEscrowTransaction);

    Decision senderDecision = new Decision(id, sender.getId(), DecisionType.UNDECIDED);
    decisionTable().insert(senderDecision);
    Decision recipientDecision = new Decision(id, recipient.getId(), DecisionType.UNDECIDED);
    decisionTable().insert(recipientDecision);
    for(Long signer : signers) {
      Decision decision = new Decision(id, signer, DecisionType.UNDECIDED);
      decisionTable().insert(decision);
    }
  }

  public static void removeEscrowTransaction(Long id) {
    Escrow escrow = escrowTable().get(escrowDbKeyFactory().newKey(id));
    if(escrow == null) {
      return;
    }
    BurstIterator<Decision> decisionIt = escrow.getDecisions();

    List<Decision> decisions = new ArrayList<>();
    while(decisionIt.hasNext()) {
      Decision decision = decisionIt.next();
      decisions.add(decision);
    }

    decisions.forEach(decision -> {
        decisionTable().delete(decision);
      });
    escrowTable().delete(escrow);
  }



  public static void updateOnBlock(Block block, int blockchainHeight) {
    Burst.getStores().getEscrowStore().updateOnBlock(block, blockchainHeight);

  }

  public final Long senderId;
  public final Long recipientId;
  public final Long id;
  public final BurstKey dbKey;
  public final Long amountNQT;
  public final int requiredSigners;
  public final int deadline;
  public final DecisionType deadlineAction;

  public Escrow(Account sender,
                Account recipient,
                Long id,
                Long amountNQT,
                int requiredSigners,
                int deadline,
                DecisionType deadlineAction) {
    this.senderId = sender.getId();
    this.recipientId = recipient.getId();
    this.id = id;
    this.dbKey = escrowDbKeyFactory().newKey(this.id);
    this.amountNQT = amountNQT;
    this.requiredSigners = requiredSigners;
    this.deadline = deadline;
    this.deadlineAction = deadlineAction;
  }

  protected Escrow( Long id,Long senderId, Long recipientId, BurstKey dbKey, Long amountNQT,
                    int requiredSigners, int deadline, DecisionType deadlineAction) {
    this.senderId = senderId;
    this.recipientId = recipientId;
    this.id = id;
    this.dbKey = dbKey;
    this.amountNQT = amountNQT;
    this.requiredSigners = requiredSigners;
    this.deadline = deadline;
    this.deadlineAction = deadlineAction;
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

  public BurstIterator<Decision> getDecisions() {
    return Burst.getStores().getEscrowStore().getDecisions(id);
  }

  public int getDeadline() {
    return deadline;
  }

  public DecisionType getDeadlineAction() {
    return deadlineAction;
  }

  public boolean isIdSigner(Long id) {
    return decisionTable().get(decisionDbKeyFactory().newKey(this.id, id)) != null;
  }

  public Decision getIdDecision(Long id) {
    return decisionTable().get(decisionDbKeyFactory().newKey(this.id, id));
  }

  public synchronized void sign(Long id, DecisionType decision) {
    if(id.equals(senderId) && decision != DecisionType.RELEASE) {
      return;
    }

    if(id.equals(recipientId) && decision != DecisionType.REFUND) {
      return;
    }

    Decision decisionChange = decisionTable().get(decisionDbKeyFactory().newKey(this.id, id));
    if(decisionChange == null) {
      return;
    }
    decisionChange.setDecision(decision);

    decisionTable().insert(decisionChange);

    if(!updatedEscrowIds().contains(this.id)) {
      updatedEscrowIds().add(this.id);
    }
  }

  public DecisionType checkComplete() {
    Decision senderDecision = decisionTable().get(decisionDbKeyFactory().newKey(id, senderId));
    if(senderDecision.getDecision() == DecisionType.RELEASE) {
      return DecisionType.RELEASE;
    }
    Decision recipientDecision = decisionTable().get(decisionDbKeyFactory().newKey(id, recipientId));
    if(recipientDecision.getDecision() == DecisionType.REFUND) {
      return DecisionType.REFUND;
    }

    int countRelease = 0;
    int countRefund = 0;
    int countSplit = 0;

    BurstIterator<Decision> decisions =Burst.getStores().getEscrowStore().getDecisions(id);
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

  public synchronized void doPayout(DecisionType result, Block block, int blockchainHeight) {
    switch(result) {
      case RELEASE:
        Account.getAccount(recipientId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
        saveResultTransaction(block, id, recipientId, amountNQT, DecisionType.RELEASE, blockchainHeight);
        break;
      case REFUND:
        Account.getAccount(senderId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT);
        saveResultTransaction(block, id, senderId, amountNQT, DecisionType.REFUND, blockchainHeight);
        break;
      case SPLIT:
        Long halfAmountNQT = amountNQT / 2;
        Account.getAccount(recipientId).addToBalanceAndUnconfirmedBalanceNQT(halfAmountNQT);
        Account.getAccount(senderId).addToBalanceAndUnconfirmedBalanceNQT(amountNQT - halfAmountNQT);
        saveResultTransaction(block, id, recipientId, halfAmountNQT, DecisionType.SPLIT, blockchainHeight);
        saveResultTransaction(block, id, senderId, amountNQT - halfAmountNQT, DecisionType.SPLIT, blockchainHeight);
        break;
      default: // should never get here
        break;
    }
  }

  private static void saveResultTransaction(Block block, Long escrowId, Long recipientId, Long amountNQT, DecisionType decision, int blockchainHeight) {
    Attachment.AbstractAttachment attachment = new Attachment.AdvancedPaymentEscrowResult(escrowId, decision, blockchainHeight);
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
    catch(BurstException.NotValidException e) {
      throw new RuntimeException(e.toString(), e);
    }

    if(!Burst.getDbs().getTransactionDb().hasTransaction(transaction.getId())) {
      resultTransactions().add(transaction);
    }
  }
}
