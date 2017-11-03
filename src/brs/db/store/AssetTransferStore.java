package brs.db.store;

import brs.AssetTransfer;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.sql.EntitySqlTable;

public interface AssetTransferStore {
    BurstKey.LongKeyFactory<AssetTransfer> getTransferDbKeyFactory();

    EntitySqlTable<AssetTransfer> getAssetTransferTable();

    BurstIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to);

    BurstIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to);

    BurstIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to);

    int getTransferCount(long assetId);
}
