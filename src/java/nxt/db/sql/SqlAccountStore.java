package nxt.db.sql;

import nxt.Account;
import nxt.Nxt;
import nxt.db.NxtIterator;
import nxt.db.VersionedBatchEntityTable;
import nxt.db.VersionedEntityTable;
import nxt.db.store.AccountStore;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jens on 10.08.2017.
 */
public class SqlAccountStore extends AccountStore {

    protected static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {
        @Override
        public DbKey newKey(Account account) {
            return account.dbKey;
        }
    };
    protected static final DbKey.LongKeyFactory<Account.RewardRecipientAssignment> rewardRecipientAssignmentDbKeyFactory = new DbKey.LongKeyFactory<Account.RewardRecipientAssignment>("account_id") {
        @Override
        public DbKey newKey(Account.RewardRecipientAssignment assignment) {
            return assignment.dbKey;
        }
    };
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlAccountStore.class);
    private static final DbKey.LinkKeyFactory<Account.AccountAsset> accountAssetDbKeyFactory = new DbKey.LinkKeyFactory<Account.AccountAsset>("account_id", "asset_id") {
        @Override
        public DbKey newKey(Account.AccountAsset accountAsset) {
            return accountAsset.dbKey;
        }

    };
    private static final VersionedEntityTable<Account.AccountAsset> accountAssetTable = new VersionedEntitySqlTable<Account.AccountAsset>("account_asset", accountAssetDbKeyFactory) {

        @Override
        protected Account.AccountAsset load(Connection con, ResultSet rs) throws SQLException {
            return new SQLAccountAsset(rs);
        }

        @Override
        protected void save(Connection con, Account.AccountAsset accountAsset) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO account_asset "
                    + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
                    + "VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, accountAsset.accountId);
                pstmt.setLong(++i, accountAsset.assetId);
                pstmt.setLong(++i, accountAsset.quantityQNT);
                pstmt.setLong(++i, accountAsset.unconfirmedQuantityQNT);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY quantity DESC, account_id, asset_id ";
        }

    };

    protected void doAccountBatch(PreparedStatement pstmt, Account account) throws SQLException {
        int i = 0;
        pstmt.setLong(++i, account.getId());
        pstmt.setInt(++i, account.getCreationHeight());
        DbUtils.setBytes(pstmt, ++i, account.getPublicKey());
        pstmt.setInt(++i, account.getKeyHeight());
        pstmt.setLong(++i, account.getBalanceNQT());
        pstmt.setLong(++i, account.getUnconfirmedBalanceNQT());
        pstmt.setLong(++i, account.getForgedBalanceNQT());
        DbUtils.setString(pstmt, ++i, account.getName());
        DbUtils.setString(pstmt, ++i, account.getDescription());
        pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
        pstmt.addBatch();
    }

    @Override
    public VersionedBatchEntityTable<Account> getAccountTable() {


        return new VersionedBatchEntitySqlTable<Account>("account", accountDbKeyFactory) {
            @Override
            protected Account load(Connection con, ResultSet rs) throws SQLException {
                return new SqlAccount(rs);
            }

        /*@Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }*/

            @Override
            protected String updateQuery() {
                return "REPLACE INTO account (id, creation_height, public_key, key_height, balance, unconfirmed_balance, " +
                        "forged_balance, name, description, height, latest) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
            }

            @Override
            protected void batch(PreparedStatement pstmt, Account account) throws SQLException {
                doAccountBatch(pstmt, account);
            }

        };
    }

    @Override
    public VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable() {
        return new VersionedEntitySqlTable<Account.RewardRecipientAssignment>("reward_recip_assign", rewardRecipientAssignmentDbKeyFactory) {

            @Override
            protected Account.RewardRecipientAssignment load(Connection con, ResultSet rs) throws SQLException {
                return new SqlRewardRecipientAssignment(rs);
            }

            @Override
            protected void save(Connection con, Account.RewardRecipientAssignment assignment) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO reward_recip_assign "
                        + "(account_id, prev_recip_id, recip_id, from_height, height, latest) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, assignment.accountId);
                    pstmt.setLong(++i, assignment.prevRecipientId);
                    pstmt.setLong(++i, assignment.recipientId);
                    pstmt.setInt(++i, assignment.fromHeight);
                    pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                    pstmt.executeUpdate();
                }
            }
        };
    }

    @Override
    public DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentDbKeyFactory() {
        return rewardRecipientAssignmentDbKeyFactory;
    }

    @Override
    public DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetDbKeyFactory() {
        return accountAssetDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<Account.AccountAsset> getAccountAssetTable() {
        return accountAssetTable;
    }

    private static class SQLAccountAsset extends Account.AccountAsset {
        public SQLAccountAsset(ResultSet rs) throws SQLException {
            super(rs.getLong("account_id"),
                    rs.getLong("asset_id"),
                    rs.getLong("quantity"),
                    rs.getLong("unconfirmed_quantity"),
                    accountAssetDbKeyFactory.newKey(rs.getLong("account_id"), rs.getLong("asset_id"))
            );
        }
    }

    protected class SqlAccount extends Account {
        SqlAccount(Long id) {
            super(id);
        }

        SqlAccount(ResultSet rs) throws SQLException {
            super(rs.getLong("id"), accountDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getInt("creation_height"));
            this.publicKey = rs.getBytes("public_key");
            this.keyHeight = rs.getInt("key_height");
            this.balanceNQT = rs.getLong("balance");
            this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
            this.forgedBalanceNQT = rs.getLong("forged_balance");
            this.name = rs.getString("name");
            this.description = rs.getString("description");
        }
    }

    protected class SqlRewardRecipientAssignment extends Account.RewardRecipientAssignment {
        protected SqlRewardRecipientAssignment(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("account_id"),
                    rs.getLong("prev_recip_id"),
                    rs.getLong("recip_id"),
                    (int) rs.getLong("from_height"),
                    rewardRecipientAssignmentDbKeyFactory.newKey(rs.getLong("account_id"))
            );
        }
    }

    @Override
    public int getAssetAccountsCount(long assetId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM account_asset WHERE asset_id = ? AND latest = TRUE")) {
            pstmt.setLong(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbKey.LongKeyFactory<Account> getAccountDbKeyFactory() {
        return accountDbKeyFactory;
    }

    private static DbClause getAccountsWithRewardRecipientClause(final long id, final int height) {
        return new DbClause(" recip_id = ? AND from_height <= ? ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, id);
                pstmt.setInt(index++, height);
                return index;
            }
        };
    }

    @Override
    public NxtIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
        return getRewardRecipientAssignmentTable().getManyBy(getAccountsWithRewardRecipientClause(recipientId, Nxt.getBlockchain().getHeight() + 1), 0, -1);
    }

    @Override
    public DbIterator<Account.AccountAsset> getAssets(int from, int to, Long id) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", id), from, to);
    }

    @Override
    public NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId),
                from, to, " ORDER BY quantity DESC, account_id ");
    }

    @Override
    public NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        if (height < 0) {
            return getAssetAccounts(assetId, from, to);
        }
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId),
                height, from, to, " ORDER BY quantity DESC, account_id ");
    }


}
