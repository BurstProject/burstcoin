package brs.db.sql;

import brs.Account;
import brs.Burst;
import brs.db.BurstIterator;
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

import static brs.schema.Tables.*;
import static org.jooq.impl.DSL.*;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.Merge;
import org.jooq.BatchBindStep;

public class SqlAccountStore implements AccountStore {

  protected static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {
      @Override
      public DbKey newKey(Account account) {
        return (DbKey) account.nxtKey;
      }
    };
  protected static final DbKey.LongKeyFactory<Account.RewardRecipientAssignment> rewardRecipientAssignmentDbKeyFactory
    = new DbKey.LongKeyFactory<Account.RewardRecipientAssignment>("account_id") {
        @Override
        public DbKey newKey(Account.RewardRecipientAssignment assignment) {
          return (DbKey) assignment.nxtKey;
        }
      };
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlAccountStore.class);
  protected static final DbKey.LinkKeyFactory<Account.AccountAsset> accountAssetDbKeyFactory
    = new DbKey.LinkKeyFactory<Account.AccountAsset>("account_id", "asset_id") {
        @Override
        public DbKey newKey(Account.AccountAsset accountAsset) {
          return (DbKey) accountAsset.nxtKey;
        }
    };

  private static Condition getAccountsWithRewardRecipientClause(final long id, final int height) {
    return REWARD_RECIP_ASSIGN.RECIP_ID.eq(id).and(REWARD_RECIP_ASSIGN.FROM_HEIGHT.le(height));
  }

  private final VersionedEntityTable<Account.AccountAsset> accountAssetTable = new VersionedEntitySqlTable<Account.AccountAsset>("account_asset", brs.schema.Tables.ACCOUNT_ASSET, accountAssetDbKeyFactory) {

      @Override
      protected Account.AccountAsset load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SQLAccountAsset(rs);
      }

      @Override
      protected void save(DSLContext ctx, Account.AccountAsset accountAsset) throws SQLException {
        ctx.mergeInto(
          ACCOUNT_ASSET,
          ACCOUNT_ASSET.ACCOUNT_ID, ACCOUNT_ASSET.ASSET_ID, ACCOUNT_ASSET.QUANTITY,
          ACCOUNT_ASSET.UNCONFIRMED_QUANTITY, ACCOUNT_ASSET.HEIGHT, ACCOUNT_ASSET.LATEST
        )
        .key(ACCOUNT_ASSET.ACCOUNT_ID, ACCOUNT_ASSET.ASSET_ID, ACCOUNT_ASSET.HEIGHT)
        .values(
          accountAsset.accountId, accountAsset.assetId, accountAsset.getQuantityQNT(),
          accountAsset.getUnconfirmedQuantityQNT(), Burst.getBlockchain().getHeight(), true
        )
        .execute();
      }

      @Override
      protected String defaultSort() {
        return " ORDER BY quantity DESC, account_id, asset_id ";
      }

    };

  VersionedEntityTable<Account.RewardRecipientAssignment> rewardRecipientAssignmentTable = new VersionedEntitySqlTable<Account.RewardRecipientAssignment>("reward_recip_assign", brs.schema.Tables.REWARD_RECIP_ASSIGN, rewardRecipientAssignmentDbKeyFactory) {

      @Override
      protected Account.RewardRecipientAssignment load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlRewardRecipientAssignment(rs);
      }

      @Override
      protected void save(DSLContext ctx, Account.RewardRecipientAssignment assignment) throws SQLException {
        ctx.mergeInto(
          REWARD_RECIP_ASSIGN,
          REWARD_RECIP_ASSIGN.ACCOUNT_ID, REWARD_RECIP_ASSIGN.PREV_RECIP_ID, REWARD_RECIP_ASSIGN.RECIP_ID,
          REWARD_RECIP_ASSIGN.FROM_HEIGHT, REWARD_RECIP_ASSIGN.HEIGHT, REWARD_RECIP_ASSIGN.LATEST
        )
        .key(REWARD_RECIP_ASSIGN.ACCOUNT_ID, REWARD_RECIP_ASSIGN.HEIGHT)
        .values(
          assignment.accountId, assignment.getPrevRecipientId(), assignment.getRecipientId(),
          assignment.getFromHeight(), Burst.getBlockchain().getHeight(), true
        )
        .execute();
      }
    };

  VersionedBatchEntityTable<Account> accountTable = new VersionedBatchEntitySqlTable<Account>("account", brs.schema.Tables.ACCOUNT, accountDbKeyFactory) {
      @Override
      protected Account load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlAccount(rs);
      }

      @Override
      protected void updateUsing(DSLContext ctx, Account account) throws SQLException {
        ctx.mergeInto(
          ACCOUNT,
          ACCOUNT.CREATION_HEIGHT, ACCOUNT.PUBLIC_KEY, ACCOUNT.KEY_HEIGHT, ACCOUNT.BALANCE,
          ACCOUNT.UNCONFIRMED_BALANCE, ACCOUNT.FORGED_BALANCE, ACCOUNT.NAME, ACCOUNT.DESCRIPTION,
          ACCOUNT.ID, ACCOUNT.HEIGHT, ACCOUNT.LATEST
        )
        .key(ACCOUNT.ID, ACCOUNT.HEIGHT)
        .values(
          account.getCreationHeight(), account.getPublicKey(), account.getKeyHeight(), account.getBalanceNQT(),
          account.getUnconfirmedBalanceNQT(), account.getForgedBalanceNQT(), account.getName(), account.getDescription(),
          account.getId(), Burst.getBlockchain().getHeight(), true
        )
        .execute();
      }
    };

  @Override
  public VersionedBatchEntityTable<Account> getAccountTable() {
    return accountTable;
  }


  @Override
  public VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable() {
    return rewardRecipientAssignmentTable;
  }

  @Override
  public DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentKeyFactory() {
    return rewardRecipientAssignmentDbKeyFactory;
  }

  @Override
  public DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetKeyFactory() {
    return accountAssetDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Account.AccountAsset> getAccountAssetTable() {
    return accountAssetTable;
  }

  @Override
  public int getAssetAccountsCount(long assetId) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return ctx.selectCount().from(ACCOUNT_ASSET).where(ACCOUNT_ASSET.ASSET_ID.eq(assetId)).and(ACCOUNT_ASSET.LATEST.isTrue()).fetchOne(0, int.class);
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public DbKey.LongKeyFactory<Account> getAccountKeyFactory() {
    return accountDbKeyFactory;
  }

  @Override
  public BurstIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
    return getRewardRecipientAssignmentTable().getManyBy(getAccountsWithRewardRecipientClause(recipientId, Burst.getBlockchain().getHeight() + 1), 0, -1);
  }

  @Override
  public BurstIterator<Account.AccountAsset> getAssets(int from, int to, Long id) {
    return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ACCOUNT_ID.eq(id), from, to);
  }

  @Override
  public BurstIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to) {
    return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ASSET_ID.eq(assetId),
                                            from, to, " ORDER BY quantity DESC, account_id ");
  }

  @Override
  public BurstIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
    if (height < 0) {
      return getAssetAccounts(assetId, from, to);
    }
    return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ASSET_ID.eq(assetId),
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
