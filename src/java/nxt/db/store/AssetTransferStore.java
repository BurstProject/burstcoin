package nxt.db.store;

import nxt.AssetTransfer;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.sql.EntitySqlTable;

public interface AssetTransferStore {
    NxtKey.LongKeyFactory<AssetTransfer> getTransferDbKeyFactory();

    EntitySqlTable<AssetTransfer> getAssetTransferTable();

    NxtIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to);

    NxtIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to);

    NxtIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to);

    int getTransferCount(long assetId);
}
