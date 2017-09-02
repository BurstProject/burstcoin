package nxt.db.sql;

import nxt.Block;
import nxt.Escrow;
import nxt.Nxt;
import nxt.TransactionImpl;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;
import nxt.db.store.EscrowStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class SqlEscrowStore implements EscrowStore {
    private final NxtKey.LongKeyFactory<Escrow> escrowDbKeyFactory = new DbKey.LongKeyFactory<Escrow>("id") {
        @Override
        public NxtKey newKey(Escrow escrow) {
            return escrow.dbKey;
        }
    };
    private final VersionedEntityTable<Escrow> escrowTable = new VersionedEntitySqlTable<Escrow>("escrow", escrowDbKeyFactory) {
        @Override
        protected Escrow load(Connection con, ResultSet rs) throws SQLException {
            return new SqlEscrow(rs);
        }

        @Override
        protected void save(Connection con, Escrow escrow) throws SQLException {
            saveEscrow(con, escrow);
        }
    };
    private final DbKey.LinkKeyFactory<Escrow.Decision> decisionDbKeyFactory =
            new DbKey.LinkKeyFactory<Escrow.Decision>("escrow_id", "account_id") {
                @Override
                public NxtKey newKey(Escrow.Decision decision) {
                    return decision.dbKey;
                }
            };
    private final VersionedEntityTable<Escrow.Decision> decisionTable = new VersionedEntitySqlTable<Escrow.Decision>("escrow_decision", decisionDbKeyFactory) {
        @Override
        protected Escrow.Decision load(Connection con, ResultSet rs) throws SQLException {
            return new SqlDecision(rs);
        }

        @Override
        protected void save(Connection con, Escrow.Decision decision) throws SQLException {
            saveDecision(con, decision);
        }
    };
    private final List<TransactionImpl> resultTransactions = new ArrayList<>();
    private final ConcurrentSkipListSet<Long> updatedEscrowIds = new ConcurrentSkipListSet<>();

    private static DbClause getUpdateOnBlockClause(final int timestamp) {
        return new DbClause(" deadline < ? ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setInt(index++, timestamp);
                return index;
            }
        };
    }

    private void saveDecision(Connection con, Escrow.Decision decision) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO escrow_decision (escrow_id, "
                + "account_id, decision, height, latest) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, decision.escrowId);
            pstmt.setLong(++i, decision.accountId);
            pstmt.setInt(++i, Escrow.decisionToByte(decision.decision));
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public NxtKey.LongKeyFactory<Escrow> getEscrowDbKeyFactory() {
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

    private DbClause getEscrowParticipentClause(final long accountId) {
        return new DbClause(" (sender_id = ? OR recipient_id = ?) ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, accountId);
                pstmt.setLong(index++, accountId);
                return index;
            }
        };
    }

    @Override
    public Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId) {
        List<Escrow> filtered = new ArrayList<>();
        DbIterator<Escrow.Decision> it = decisionTable.getManyBy(new DbClause.LongClause("account_id", accountId), 0, -1);
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

        DbIterator<Escrow> deadlineEscrows = escrowTable.getManyBy(getUpdateOnBlockClause(block.getTimestamp()), 0, -1);
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
                    Nxt.getDbs().getTransactionDb().saveTransactions(con, resultTransactions);
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
        NxtIterator<Escrow.Decision> decisionIt = escrow.getDecisions();

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

    private void saveEscrow(Connection con, Escrow escrow) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO escrow (id, sender_id, "
                + "recipient_id, amount, required_signers, deadline, deadline_action, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, escrow.id);
            pstmt.setLong(++i, escrow.senderId);
            pstmt.setLong(++i, escrow.recipientId);
            pstmt.setLong(++i, escrow.amountNQT);
            pstmt.setInt(++i, escrow.requiredSigners);
            pstmt.setInt(++i, escrow.deadline);
            pstmt.setInt(++i, Escrow.decisionToByte(escrow.deadlineAction));
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
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
    public 	NxtIterator<Escrow.Decision> getDecisions(Long id)
    {
        return  decisionTable.getManyBy(new DbClause.LongClause("escrow_id", id), 0, -1);
    }

}
