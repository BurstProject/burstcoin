package brs.db.sql;

import brs.*;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.DerivedTableManager;
import brs.db.store.EscrowStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.Field;

import static brs.schema.Tables.ESCROW;
import static brs.schema.Tables.ESCROW_DECISION;

public class SqlEscrowStore implements EscrowStore {
  private final BurstKey.LongKeyFactory<Escrow> escrowDbKeyFactory = new DbKey.LongKeyFactory<Escrow>("id") {
      @Override
      public BurstKey newKey(Escrow escrow) {
        return escrow.dbKey;
      }
    };

  private final VersionedEntityTable<Escrow> escrowTable;
  private final DbKey.LinkKeyFactory<Escrow.Decision> decisionDbKeyFactory =
      new DbKey.LinkKeyFactory<Escrow.Decision>("escrow_id", "account_id") {
        @Override
        public BurstKey newKey(Escrow.Decision decision) {
          return decision.dbKey;
        }
      };
  private final VersionedEntityTable<Escrow.Decision> decisionTable;
  private final List<TransactionImpl> resultTransactions = new ArrayList<>();
  private final ConcurrentSkipListSet<Long> updatedEscrowIds = new ConcurrentSkipListSet<>();

  public SqlEscrowStore(DerivedTableManager derivedTableManager) {
    escrowTable = new VersionedEntitySqlTable<Escrow>("escrow", brs.schema.Tables.ESCROW, escrowDbKeyFactory, derivedTableManager) {
      @Override
      protected Escrow load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlEscrow(rs);
      }

      @Override
      protected void save(DSLContext ctx, Escrow escrow) throws SQLException {
        saveEscrow(ctx, escrow);
      }
    };

    decisionTable = new VersionedEntitySqlTable<Escrow.Decision>("escrow_decision", brs.schema.Tables.ESCROW_DECISION, decisionDbKeyFactory, derivedTableManager) {
      @Override
      protected Escrow.Decision load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlDecision(rs);
      }

      @Override
      protected void save(DSLContext ctx, Escrow.Decision decision) throws SQLException {
        saveDecision(ctx, decision);
      }
    };
  }

  private static Condition getUpdateOnBlockClause(final int timestamp) {
    return ESCROW.DEADLINE.lt(timestamp);
  }

  protected void saveDecision(DSLContext ctx, Escrow.Decision decision) throws SQLException {
    brs.schema.tables.records.EscrowDecisionRecord decisionRecord = ctx.newRecord(ESCROW_DECISION);
    decisionRecord.setEscrowId(decision.escrowId);
    decisionRecord.setAccountId(decision.accountId);
    decisionRecord.setDecision(((int) Escrow.decisionToByte(decision.getDecision())));
    decisionRecord.setHeight(Burst.getBlockchain().getHeight());
    decisionRecord.setLatest(true);
    DbUtils.mergeInto(
      ctx, decisionRecord, ESCROW_DECISION,
      ( new Field[] { decisionRecord.field("escrow_id"), decisionRecord.field("account_id"), decisionRecord.field("height") } )
    );
  }

  @Override
  public BurstKey.LongKeyFactory<Escrow> getEscrowDbKeyFactory() {
    return escrowDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Escrow> getEscrowTable() {
    return escrowTable;
  }

  @Override
  public DbKey.LinkKeyFactory<Escrow.Decision> getDecisionDbKeyFactory() {
    return decisionDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Escrow.Decision> getDecisionTable() {
    return decisionTable;
  }

  private Condition getEscrowParticipentClause(final long accountId) {
    return ESCROW.SENDER_ID.eq(accountId).or(ESCROW.RECIPIENT_ID.eq(accountId));
  }

  @Override
  public Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId) {
    List<Escrow> filtered = new ArrayList<>();
    BurstIterator<Escrow.Decision> it = decisionTable.getManyBy(ESCROW_DECISION.ACCOUNT_ID.eq(accountId), 0, -1);
    while (it.hasNext()) {
      Escrow.Decision decision = it.next();
      Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(decision.escrowId));
      if (escrow != null) {
        filtered.add(escrow);
      }
    }
    return filtered;
  }

  @Override
  public void updateOnBlock(Block block, int blockchainHeight) {
    resultTransactions.clear();

    BurstIterator<Escrow> deadlineEscrows = escrowTable.getManyBy(getUpdateOnBlockClause(block.getTimestamp()), 0, -1);
    while(deadlineEscrows.hasNext()) {
      updatedEscrowIds.add(deadlineEscrows.next().getId());
    }

    if (updatedEscrowIds.size() > 0) {
      for (Long escrowId : updatedEscrowIds) {
        Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(escrowId));
        Escrow.DecisionType result = escrow.checkComplete();
        if (result != Escrow.DecisionType.UNDECIDED || escrow.getDeadline() < block.getTimestamp()) {
          if (result == Escrow.DecisionType.UNDECIDED) {
            result = escrow.getDeadlineAction();
          }
          escrow.doPayout(result, block, blockchainHeight);

          removeEscrowTransaction(escrowId);
        }
      }
      if (resultTransactions.size() > 0) {
        Burst.getDbs().getTransactionDb().saveTransactions( resultTransactions);
      }
      updatedEscrowIds.clear();
    }
  }

  @Override
  public List<TransactionImpl> getResultTransactions() {
    return resultTransactions;
  }

  @Override
  public ConcurrentSkipListSet<Long> getUpdatedEscrowIds() {
    return updatedEscrowIds;
  }

  public void removeEscrowTransaction(Long id) {
    Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(id));
    if (escrow == null) {
      return;
    }
    BurstIterator<Escrow.Decision> decisionIt = escrow.getDecisions();

    List<Escrow.Decision> decisions = new ArrayList<>();
    while (decisionIt.hasNext()) {
      Escrow.Decision decision = decisionIt.next();
      decisions.add(decision);
    }

    for (Escrow.Decision decision : decisions) {
      decisionTable.delete(decision);
    }
    escrowTable.delete(escrow);
  }

  protected void saveEscrow(DSLContext ctx, Escrow escrow) throws SQLException {
    brs.schema.tables.records.EscrowRecord escrowRecord = ctx.newRecord(ESCROW);
    escrowRecord.setId(escrow.id);
    escrowRecord.setSenderId(escrow.senderId);
    escrowRecord.setRecipientId(escrow.recipientId);
    escrowRecord.setAmount(escrow.amountNQT);
    escrowRecord.setRequiredSigners(escrow.requiredSigners);
    escrowRecord.setDeadline(escrow.deadline);
    escrowRecord.setDeadlineAction(((int) escrow.decisionToByte(escrow.deadlineAction)));
    escrowRecord.setHeight(Burst.getBlockchain().getHeight());
    escrowRecord.setLatest(true);
    DbUtils.mergeInto(
      ctx, escrowRecord, ESCROW,
      ( new Field[] { escrowRecord.field("id"), escrowRecord.field("height") } )
    );
  }

  private class SqlDecision extends Escrow.Decision {
    private SqlDecision(ResultSet rs) throws SQLException {
      super(rs.getLong("escrow_id"), rs.getLong("account_id"),
            Escrow.byteToDecision((byte) rs.getInt("decision")));
    }
  }

  private class SqlEscrow extends Escrow {
    private SqlEscrow(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("sender_id"),
            rs.getLong("recipient_id"),
            escrowDbKeyFactory.newKey(rs.getLong("id")),
            rs.getLong("amount"),
            rs.getInt("required_signers"),
            rs.getInt("deadline"),
            byteToDecision((byte) rs.getInt("deadline_action"))
            );
    }
  }

  @Override
  public 	BurstIterator<Escrow.Decision> getDecisions(Long id)
  {
    return  decisionTable.getManyBy(ESCROW_DECISION.ESCROW_ID.eq(id), 0, -1);
  }

}
