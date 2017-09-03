package nxt.db.store;

import nxt.Asset;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.sql.EntitySqlTable;

public interface AssetStore {
    NxtKey.LongKeyFactory<Asset> getAssetDbKeyFactory();

    EntitySqlTable<Asset> getAssetTable();

    NxtIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);
}
