package nxt.db.mariadb;

import nxt.Account;
import nxt.Nxt;
import nxt.db.VersionedBatchEntityTable;
import nxt.db.VersionedEntityTable;
import nxt.db.sql.SqlAccountStore;
import nxt.db.sql.VersionedBatchEntitySqlTable;
import nxt.db.sql.VersionedEntitySqlTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jens on 10.08.2017.
 */
class MariadbAccountStore extends SqlAccountStore {


    private final VersionedEntityTable<Account.AccountAsset> accountAssetTable = new VersionedEntitySqlTable<Account.AccountAsset>("account_asset", accountAssetDbKeyFactory) {

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

    @Override
    public VersionedEntityTable<Account.AccountAsset> getAccountAssetTable() {
        return accountAssetTable;
    }

    @Override
    public VersionedBatchEntityTable<Account> getAccountTable() {


        return new VersionedBatchEntitySqlTable<Account>("account", accountDbKeyFactory) {
            @Override
            protected Account load(Connection con, ResultSet rs) throws SQLException {
                return new SqlAccount(rs);
            }

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
}
