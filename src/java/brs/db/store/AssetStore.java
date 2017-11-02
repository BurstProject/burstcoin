package brs.db.store;

import brs.Asset;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.sql.EntitySqlTable;

public interface AssetStore {
    NxtKey.LongKeyFactory<Asset> getAssetDbKeyFactory();

    EntitySqlTable<Asset> getAssetTable();

    NxtIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);
}
