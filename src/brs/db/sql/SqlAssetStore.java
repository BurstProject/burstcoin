package brs.db.sql;

import brs.Asset;
import brs.Burst;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.store.AssetStore;
import org.jooq.DSLContext;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jooq.DSLContext;
import static brs.schema.tables.Asset.ASSET;

public class SqlAssetStore implements AssetStore {

  private final BurstKey.LongKeyFactory<Asset> assetDbKeyFactory = new DbKey.LongKeyFactory<Asset>("id") {

      @Override
      public BurstKey newKey(Asset asset) {
        return asset.dbKey;
      }

    };
  private final EntitySqlTable<Asset> assetTable = new EntitySqlTable<Asset>("asset", brs.schema.Tables.ASSET, assetDbKeyFactory) {

      @Override
      protected Asset load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlAsset(rs);
      }

      @Override
      protected void save(DSLContext ctx, Asset asset) throws SQLException {
        saveAsset(ctx, asset);
      }
    };

  private void saveAsset(DSLContext ctx, Asset asset) throws SQLException {
    ctx.insertInto(ASSET).
      set(ASSET.ID, asset.getId()).
      set(ASSET.ACCOUNT_ID, asset.getAccountId()).
      set(ASSET.NAME, asset.getName()).
      set(ASSET.DESCRIPTION, asset.getDescription()).
      set(ASSET.QUANTITY, asset.getQuantityQNT()).
      set(ASSET.DECIMALS, asset.getDecimals()).
      set(ASSET.HEIGHT, Burst.getBlockchain().getHeight()).execute();
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
    return assetTable.getManyBy(ASSET.ACCOUNT_ID.eq(accountId), from, to);
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
