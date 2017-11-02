package brs.db.sql;

import brs.AT;
import brs.Nxt;
import brs.at.AT_API_Helper;
import brs.at.AT_Constants;
import brs.db.NxtKey;
import brs.db.VersionedEntityTable;
import brs.db.store.ATStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SqlATStore implements ATStore {

    private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);

    private final NxtKey.LongKeyFactory<AT> atDbKeyFactory = new DbKey.LongKeyFactory<AT>("id") {
        @Override
        public NxtKey newKey(AT at) {
            return at.dbKey;
        }
    };
    private final VersionedEntityTable<AT> atTable = new VersionedEntitySqlTable<AT>("at", atDbKeyFactory) {
        @Override
        protected AT load(Connection con, ResultSet rs) throws SQLException {
            //return new AT(rs);
            throw new RuntimeException("AT attempted to be created with atTable.load");
        }

        @Override
        protected void save(Connection con, AT at) throws SQLException {
            saveAT(con, at);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY id ";
        }
    };
    private final NxtKey.LongKeyFactory<AT.ATState> atStateDbKeyFactory = new DbKey.LongKeyFactory<AT.ATState>("at_id") {
        @Override
        public NxtKey newKey(AT.ATState atState) {
            return atState.dbKey;
        }
    };

    private final VersionedEntityTable<AT.ATState> atStateTable = new VersionedEntitySqlTable<AT.ATState>("at_state", atStateDbKeyFactory) {
        @Override
        protected AT.ATState load(Connection con, ResultSet rs) throws SQLException {
            return new SqlATState(rs);
        }

        @Override
        protected void save(Connection con, AT.ATState atState) throws SQLException {
            saveATState(con, atState);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY prev_height, height, at_id ";
        }
    };

    protected abstract void saveATState(Connection con, AT.ATState atState) throws SQLException;

    protected abstract void saveAT(Connection con, AT at) throws SQLException;

    @Override
    public boolean isATAccountId(Long id) {
        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT id FROM " + DbUtils.quoteTableName("at") + " WHERE id = ? AND latest = TRUE")) {
            pstmt.setLong(1, id);
            ResultSet result = pstmt.executeQuery();
            return result.next();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Long> getOrderedATs() {
        List<Long> orderedATs = new ArrayList<>();
        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT at.id FROM " + DbUtils.quoteTableName("at") + " "
                     + "INNER JOIN at_state ON at.id = at_state.at_id INNER JOIN account ON at.id = account.id "
                     + "WHERE at.latest = TRUE AND at_state.latest = TRUE AND account.latest = TRUE "
                     + "AND at_state.next_height <= ? AND account.balance >= ? "
                     + "AND (at_state.freeze_when_same_balance = FALSE OR (account.balance - at_state.prev_balance >= at_state.min_activate_amount)) "
                     + "ORDER BY at_state.prev_height, at_state.next_height, at.id")) {
            pstmt.setInt(1, Nxt.getBlockchain().getHeight() + 1);
            pstmt.setLong(2, AT_Constants.getInstance().STEP_FEE(Nxt.getBlockchain().getHeight()) *
                    AT_Constants.getInstance().API_STEP_MULTIPLIER(Nxt.getBlockchain().getHeight()));
            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                Long id = result.getLong(1);
                orderedATs.add(id);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return orderedATs;
    }

    @Override
    public AT getAT(Long id) {
        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT t_at.id, t_at.creator_id, t_at.name, t_at.description, t_at.version, "
                     + "at_state.state, t_at.csize, t_at.dsize, t_at.c_user_stack_bytes, t_at.c_call_stack_bytes, "
                     + "t_at.creation_height, at_state.sleep_between, at_state.next_height, at_state.freeze_when_same_balance, at_state.min_activate_amount, "
                     + "t_at.ap_code "
                     + "FROM " +  DbUtils.quoteTableName("at") + " AS t_at INNER JOIN at_state ON t_at.id = at_state.at_id "
                     + "WHERE t_at.latest = TRUE AND at_state.latest = TRUE "
                     + "AND t_at.id = ?")) {
            int i = 0;
            pstmt.setLong(++i, id);
            ResultSet result = pstmt.executeQuery();
            List<AT> ats = createATs(result);
            if (ats.size() > 0) {
                return ats.get(0);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Long> getATsIssuedBy(Long accountId) {
        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT id "
                     + "FROM " + DbUtils.quoteTableName("at") + " "
                     + "WHERE latest = TRUE AND creator_id = ? "
                     + "ORDER BY creation_height DESC, id")) {
            pstmt.setLong(1, accountId);
            ResultSet result = pstmt.executeQuery();
            List<Long> resultList = new ArrayList<>();
            while (result.next()) {
                resultList.add(result.getLong(1));
            }
            return resultList;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Collection<Long> getAllATIds() {
        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT id FROM " + DbUtils.quoteTableName("at") + " WHERE latest = TRUE")) {
            ResultSet result = pstmt.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (result.next()) {
                ids.add(result.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public NxtKey.LongKeyFactory<AT> getAtDbKeyFactory() {
        return atDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<AT> getAtTable() {
        return atTable;
    }

    @Override
    public NxtKey.LongKeyFactory<AT.ATState> getAtStateDbKeyFactory() {
        return atStateDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<AT.ATState> getAtStateTable() {
        return atStateTable;
    }

    protected List<AT> createATs(ResultSet rs) throws SQLException {
        List<AT> ats = new ArrayList<AT>();
        while (rs.next()) {
            int i = 0;
            Long atId = rs.getLong(++i);
            Long creator = rs.getLong(++i);
            String name = rs.getString(++i);
            String description = rs.getString(++i);
            short version = rs.getShort(++i);
            byte[] stateBytes = AT.decompressState(rs.getBytes(++i));
            int csize = rs.getInt(++i);
            int dsize = rs.getInt(++i);
            int c_user_stack_bytes = rs.getInt(++i);
            int c_call_stack_bytes = rs.getInt(++i);
            int creationBlockHeight = rs.getInt(++i);
            int sleepBetween = rs.getInt(++i);
            int nextHeight = rs.getInt(++i);
            boolean freezeWhenSameBalance = rs.getBoolean(++i);
            long minActivationAmount = rs.getLong(++i);
            byte[] ap_code = AT.decompressState(rs.getBytes(++i));

            AT at = new AT(AT_API_Helper.getByteArray(atId), AT_API_Helper.getByteArray(creator), name, description, version,
                    stateBytes, csize, dsize, c_user_stack_bytes, c_call_stack_bytes, creationBlockHeight, sleepBetween, nextHeight,
                    freezeWhenSameBalance, minActivationAmount, ap_code);
            ats.add(at);

        }
        return ats;
    }

    @Override
    public Long findTransaction(int startHeight, int endHeight, Long atID, int numOfTx, long minAmount) {
        logger.debug(
            "findTransaction: "
            + "SELECT id FROM transaction WHERE"
            + " height >= "          + startHeight
            + " AND height < "       + endHeight
            + " AND recipient_id = " + atID
            + " AND amount >=  "     + minAmount
            + " ORDER BY height, id"
            + DbUtils.limitsClause(numOfTx, numOfTx + 1)
            + " -- ? = " + numOfTx
            + ", ? = "   + ( numOfTx + 1 )
        );

        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT id FROM transaction "
                     + "WHERE height >= ? AND height < ? AND recipient_id = ? AND amount >= ? "
                     + "ORDER BY height, id"
                     + DbUtils.limitsClause(numOfTx, numOfTx + 1) ) ) {
            int i = 1;
            pstmt.setInt(i++, startHeight);
            pstmt.setInt(i++, endHeight);
            pstmt.setLong(i++, atID);
            pstmt.setLong(i++, minAmount);
            i = DbUtils.setLimits(i++, pstmt, numOfTx, numOfTx + 1);
            ResultSet rs = pstmt.executeQuery();
            Long transactionId = 0L;
            if (rs.next()) {
                transactionId = rs.getLong("id");
            }
            rs.close();
            return transactionId;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }

    }

    @Override
    public int findTransactionHeight(Long transactionId, int height, Long atID, long minAmount) {
        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT id FROM transaction "
                     + "WHERE height= ? and recipient_id = ? AND amount >= ? "
                     + "ORDER BY height, id")) {
            pstmt.setInt(1, height);
            pstmt.setLong(2, atID);
            pstmt.setLong(3, minAmount);
            ResultSet rs = pstmt.executeQuery();

            int counter = 0;
            while (rs.next()) {
                if (rs.getLong("id") == transactionId) {
                    counter++;
                    break;
                }
                counter++;
            }
            rs.close();
            return counter;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    protected class SqlATState extends AT.ATState {
        private SqlATState(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("at_id"),
                    rs.getBytes("state"),
                    rs.getInt("prev_height"),
                    rs.getInt("next_height"),
                    rs.getInt("sleep_between"),
                    rs.getLong("prev_balance"),
                    rs.getBoolean("freeze_when_same_balance"),
                    rs.getLong("min_activate_amount")
            );
        }
    }
}
