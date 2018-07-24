package brs.db.store;

import brs.Asset;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.sql.EntitySqlTable;

public interface AssetStore {
  BurstKey.LongKeyFactory<Asset> getAssetDbKeyFactory();

  EntitySqlTable<Asset> getAssetTable();

  BurstIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);
}
