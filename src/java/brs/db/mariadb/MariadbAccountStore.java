package brs.db.mariadb;

import brs.Account;
import brs.Burst;
import brs.db.VersionedBatchEntityTable;
import brs.db.VersionedEntityTable;
import brs.db.sql.DbUtils;
import brs.db.sql.SqlAccountStore;
import brs.db.sql.VersionedBatchEntitySqlTable;
import brs.db.sql.VersionedEntitySqlTable;

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
          pstmt.setInt(++i, Burst.getBlockchain().getHeight());
          pstmt.executeUpdate();
        }
      }

      @Override
      protected String defaultSort() {
        return " ORDER BY quantity DESC, account_id, asset_id ";
      }

    };
  VersionedBatchEntityTable<Account> accountTable = new VersionedBatchEntitySqlTable<Account>("account", accountDbKeyFactory) {
      @Override
      protected Account load(Connection con, ResultSet rs) throws SQLException {
        return new SqlAccount(rs);
      }

      @Override
      protected String updateQuery() {
        return "REPLACE INTO account (creation_height, public_key, key_height, balance, unconfirmed_balance, " +
        "forged_balance, name, description, id, height, latest) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
      }

      @Override
      protected void batch(PreparedStatement pstmt, Account account) throws SQLException {
        doMariadbAccountBatch(pstmt, account);
      }

      void doMariadbAccountBatch(PreparedStatement pstmt, Account account) throws SQLException {
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
        pstmt.setInt(++i, Burst.getBlockchain().getHeight());

        //            // UPDATE-Part
        //            pstmt.setInt(++i, account.getCreationHeight());
        //            DbUtils.setBytes(pstmt, ++i, account.getPublicKey());
        //            pstmt.setInt(++i, account.getKeyHeight());
        //            pstmt.setLong(++i, account.getBalanceNQT());
        //            pstmt.setLong(++i, account.getUnconfirmedBalanceNQT());
        //            pstmt.setLong(++i, account.getForgedBalanceNQT());
        //            DbUtils.setString(pstmt, ++i, account.getName());
        //            DbUtils.setString(pstmt, ++i, account.getDescription());

        pstmt.addBatch();
      }
    };

  @Override
  public VersionedEntityTable<Account.AccountAsset> getAccountAssetTable() {
    return accountAssetTable;
  }

  @Override
  public VersionedBatchEntityTable<Account> getAccountTable() {
    return accountTable;
  }

  private  VersionedEntitySqlTable<Account.RewardRecipientAssignment> rewardRecipientAssignmentVersionedEntitySqlTable =  new VersionedEntitySqlTable<Account.RewardRecipientAssignment>("reward_recip_assign", rewardRecipientAssignmentDbKeyFactory) {

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
          pstmt.setInt(++i, Burst.getBlockchain().getHeight());
          pstmt.executeUpdate();
        }
      }
    };

  @Override
  public VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable() {
    return rewardRecipientAssignmentVersionedEntitySqlTable;
  }
}
