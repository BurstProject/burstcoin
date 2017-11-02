package brs.db.sql;

import brs.Account;
import brs.Nxt;
import brs.db.NxtIterator;
import brs.db.VersionedBatchEntityTable;
import brs.db.VersionedEntityTable;
import brs.db.store.AccountStore;
import brs.util.Convert;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by jens on 10.08.2017.
 */
public abstract class SqlAccountStore implements AccountStore {

    protected static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {
        @Override
        public DbKey newKey(Account account) {
            return (DbKey) account.nxtKey;
        }
    };
    protected static final DbKey.LongKeyFactory<Account.RewardRecipientAssignment> rewardRecipientAssignmentDbKeyFactory = new DbKey.LongKeyFactory<Account.RewardRecipientAssignment>("account_id") {
        @Override
        public DbKey newKey(Account.RewardRecipientAssignment assignment) {
            return (DbKey) assignment.nxtKey;
        }
    };
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlAccountStore.class);
    protected static final DbKey.LinkKeyFactory<Account.AccountAsset> accountAssetDbKeyFactory = new DbKey.LinkKeyFactory<Account.AccountAsset>("account_id", "asset_id") {
        @Override
        public DbKey newKey(Account.AccountAsset accountAsset) {
            return (DbKey) accountAsset.nxtKey;
        }

    };

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

    protected void doAccountBatch(PreparedStatement pstmt, Account account) throws SQLException {
        int i = 0;

        pstmt.setInt(++i, account.getCreationHeight());
        DbUtils.setBytes(pstmt, ++i, account.getPublicKey());
        pstmt.setInt(++i, account.getKeyHeight());
        pstmt.setLong(++i, account.getBalanceNQT());
        pstmt.setLong(++i, account.getUnconfirmedBalanceNQT());
        pstmt.setLong(++i, account.getForgedBalanceNQT());
        DbUtils.setString(pstmt, ++i, account.getName());
        DbUtils.setString(pstmt, ++i, account.getDescription());
        pstmt.setLong(++i, account.getId());
        pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
        pstmt.addBatch();
    }

    @Override
    public abstract VersionedBatchEntityTable<Account> getAccountTable();

    @Override
    public abstract VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable();

    @Override
    public DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentKeyFactory() {
        return rewardRecipientAssignmentDbKeyFactory;
    }

    @Override
    public DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetKeyFactory() {
        return accountAssetDbKeyFactory;
    }

    @Override
    public abstract VersionedEntityTable<Account.AccountAsset> getAccountAssetTable();

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
    public DbKey.LongKeyFactory<Account> getAccountKeyFactory() {
        return accountDbKeyFactory;
    }

    @Override
    public NxtIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
        return getRewardRecipientAssignmentTable().getManyBy(getAccountsWithRewardRecipientClause(recipientId, Nxt.getBlockchain().getHeight() + 1), 0, -1);
    }

    @Override
    public NxtIterator<Account.AccountAsset> getAssets(int from, int to, Long id) {
        return getAccountAssetTable().getManyBy(new DbClause.LongClause("account_id", id), from, to);
    }

    @Override
    public NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return getAccountAssetTable().getManyBy(new DbClause.LongClause("asset_id", assetId),
                from, to, " ORDER BY quantity DESC, account_id ");
    }

    @Override
    public NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        if (height < 0) {
            return getAssetAccounts(assetId, from, to);
        }
        return getAccountAssetTable().getManyBy(new DbClause.LongClause("asset_id", assetId),
                height, from, to, " ORDER BY quantity DESC, account_id ");
    }

    @Override
    public boolean setOrVerify(Account acc, byte[] key, int height) {
        if (acc.publicKey == null) {
            if (Db.isInTransaction()) {
                acc.publicKey = key;
                acc.keyHeight = -1;
                getAccountTable().insert(acc);
            }
            return true;
        } else if (Arrays.equals(acc.publicKey, key)) {
            return true;
        } else if (acc.keyHeight == -1) {
            logger.info("DUPLICATE KEY!!!");
            logger.info("Account key for " + Convert.toUnsignedLong(acc.id) + " was already set to a different one at the same height "
                    + ", current height is " + height + ", rejecting new key");
            return false;
        } else if (acc.keyHeight >= height) {
            logger.info("DUPLICATE KEY!!!");
            if (Db.isInTransaction()) {
                logger.info("Changing key for account " + Convert.toUnsignedLong(acc.id) + " at height " + height
                        + ", was previously set to a different one at height " + acc.keyHeight);
                acc.publicKey = key;
                acc.keyHeight = height;
                getAccountTable().insert(acc);
            }
            return true;
        }
        logger.info("DUPLICATE KEY!!!");
        logger.info("Invalid key for account " + Convert.toUnsignedLong(acc.id) + " at height " + height
                + ", was already set to a different one at height " + acc.keyHeight);
        return false;
    }

    protected static class SQLAccountAsset extends Account.AccountAsset {
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

        public SqlAccount(ResultSet rs) throws SQLException {
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
        public SqlRewardRecipientAssignment(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("account_id"),
                    rs.getLong("prev_recip_id"),
                    rs.getLong("recip_id"),
                    (int) rs.getLong("from_height"),
                    rewardRecipientAssignmentDbKeyFactory.newKey(rs.getLong("account_id"))
            );
        }
    }


}
