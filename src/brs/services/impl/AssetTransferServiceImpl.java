package brs.services.impl;

import brs.AssetTransfer;
import brs.db.BurstIterator;
import brs.db.store.AssetTransferStore;
import brs.services.AssetTransferService;

public class AssetTransferServiceImpl implements AssetTransferService {

  private final AssetTransferStore assetTransferStore;

  public AssetTransferServiceImpl(AssetTransferStore assetTransferStore) {
    this.assetTransferStore = assetTransferStore;
  }

  @Override
  public BurstIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
    return assetTransferStore.getAssetTransfers(assetId, from, to);
  }

  @Override
  public BurstIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
    return assetTransferStore.getAccountAssetTransfers(accountId, assetId, from, to);
  }

}
