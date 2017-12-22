package brs.db.sql;

import brs.Asset;
import brs.Burst;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.store.AssetStore;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static brs.schema.tables.Asset.ASSET;

public abstract  class SqlAssetStore implements AssetStore {

  private final BurstKey.LongKeyFactory<Asset> assetDbKeyFactory = new DbKey.LongKeyFactory<Asset>("id") {

      @Override
      public BurstKey newKey(Asset asset) {
        return asset.dbKey;
      }

    };
  private final EntitySqlTable<Asset> assetTable = new EntitySqlTable<Asset>("asset", brs.schema.Tables.ASSET, assetDbKeyFactory) {

      @Override
      protected Asset load(Connection con, ResultSet rs) throws SQLException {
        return new SqlAsset(rs);
      }

      @Override
      protected void save(Connection con, Asset asset) throws SQLException {
        saveAsset(asset);
      }
    };

  private void saveAsset(Asset asset) throws SQLException {
    try (DSLContext ctx = Db.getDSLContext()) {
      ctx.insertInto(ASSET).
              set(ASSET.ID, asset.getId()).
              set(ASSET.ACCOUNT_ID, asset.getAccountId()).
              set(ASSET.NAME, asset.getName()).
              set(ASSET.DESCRIPTION, asset.getDescription()).
              set(ASSET.QUANTITY, asset.getQuantityQNT()).
              set(ASSET.DECIMALS, asset.getDecimals()).
              set(ASSET.HEIGHT, Burst.getBlockchain().getHeight()).execute();
    }
  }

  @Override
  public BurstKey.LongKeyFactory<Asset> getAssetDbKeyFactory() {
    return assetDbKeyFactory;
  }

  @Override
  public EntitySqlTable<Asset> getAssetTable() {
    return assetTable;
  }

  @Override
  public BurstIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
    return assetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
  }

  private class SqlAsset extends Asset {

    private SqlAsset(ResultSet rs) throws SQLException {
      super(rs.getLong("id"),
            assetDbKeyFactory.newKey(rs.getLong("id")),
            rs.getLong("account_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getLong("quantity"),
            rs.getByte("decimals")
            );
    }
  }
}
