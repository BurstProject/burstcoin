package brs.db.sql;

import brs.*;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.EscrowStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jooq.DSLContext;
import org.jooq.Condition;

import static brs.schema.Tables.ESCROW;
import static brs.schema.Tables.ESCROW_DECISION;

public class SqlEscrowStore implements EscrowStore {
  private final BurstKey.LongKeyFactory<Escrow> escrowDbKeyFactory = new DbKey.LongKeyFactory<Escrow>("id") {
      @Override
      public BurstKey newKey(Escrow escrow) {
        return escrow.dbKey;
      }
    };
  private final VersionedEntityTable<Escrow> escrowTable = new VersionedEntitySqlTable<Escrow>("escrow", brs.schema.Tables.ESCROW, escrowDbKeyFactory) {
      @Override
      protected Escrow load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlEscrow(rs);
      }

      @Override
      protected void save(DSLContext ctx, Escrow escrow) throws SQLException {
        saveEscrow(ctx, escrow);
      }
    };
  private final DbKey.LinkKeyFactory<Escrow.Decision> decisionDbKeyFactory =
      new DbKey.LinkKeyFactory<Escrow.Decision>("escrow_id", "account_id") {
        @Override
        public BurstKey newKey(Escrow.Decision decision) {
          return decision.dbKey;
        }
      };
  private final VersionedEntityTable<Escrow.Decision> decisionTable = new VersionedEntitySqlTable<Escrow.Decision>("escrow_decision", brs.schema.Tables.ESCROW_DECISION, decisionDbKeyFactory) {
      @Override
      protected Escrow.Decision load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlDecision(rs);
      }

      @Override
      protected void save(DSLContext ctx, Escrow.Decision decision) throws SQLException {
        saveDecision(ctx, decision);
      }
    };
  private final List<TransactionImpl> resultTransactions = new ArrayList<>();
  private final ConcurrentSkipListSet<Long> updatedEscrowIds = new ConcurrentSkipListSet<>();

  private static Condition getUpdateOnBlockClause(final int timestamp) {
    return ESCROW.DEADLINE.lt(timestamp);
  }

  protected void saveDecision(DSLContext ctx, Escrow.Decision decision) throws SQLException {
    ctx.mergeInto(
      ESCROW_DECISION,
      ESCROW_DECISION.ESCROW_ID, ESCROW_DECISION.ACCOUNT_ID, ESCROW_DECISION.DECISION,
      ESCROW_DECISION.HEIGHT, ESCROW_DECISION.LATEST
    )
    .key(ESCROW_DECISION.ESCROW_ID, ESCROW_DECISION.ACCOUNT_ID, ESCROW_DECISION.HEIGHT)
    .values(
      decision.escrowId, decision.accountId, ((int) Escrow.decisionToByte(decision.getDecision())),
      Burst.getBlockchain().getHeight(), true
    )
    .execute();
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
  public void updateOnBlock(Block block) {
    resultTransactions.clear();

    BurstIterator<Escrow> deadlineEscrows = escrowTable.getManyBy(getUpdateOnBlockClause(block.getTimestamp()), 0, -1);
    for (Escrow escrow : deadlineEscrows) {
      updatedEscrowIds.add(escrow.getId());
    }

    if (updatedEscrowIds.size() > 0) {
      for (Long escrowId : updatedEscrowIds) {
        Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(escrowId));
        Escrow.DecisionType result = escrow.checkComplete();
        if (result != Escrow.DecisionType.UNDECIDED || escrow.getDeadline() < block.getTimestamp()) {
          if (result == Escrow.DecisionType.UNDECIDED) {
            result = escrow.getDeadlineAction();
          }
          escrow.doPayout(result, block);

          removeEscrowTransaction(escrowId);
        }
      }
      if (resultTransactions.size() > 0) {
        try (Connection con = Db.getConnection()) {
          Burst.getDbs().getTransactionDb().saveTransactions( resultTransactions);
        } catch (SQLException e) {
          throw new RuntimeException(e.toString(), e);
        }
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
    ctx.mergeInto(
      ESCROW,
      ESCROW.ID, ESCROW.SENDER_ID, ESCROW.RECIPIENT_ID, ESCROW.AMOUNT, ESCROW.REQUIRED_SIGNERS,
      ESCROW.DEADLINE, ESCROW.DEADLINE_ACTION, ESCROW.HEIGHT, ESCROW.LATEST
    )
    .key(ESCROW.ID, ESCROW.HEIGHT)
    .values(
      escrow.id, escrow.senderId, escrow.recipientId, escrow.amountNQT, escrow.requiredSigners,
      escrow.deadline, ((int) escrow.decisionToByte(escrow.deadlineAction)), Burst.getBlockchain().getHeight(), true
    )
    .execute();
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
