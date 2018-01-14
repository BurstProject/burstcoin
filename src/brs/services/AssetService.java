package brs.services;

import brs.Account.AccountAsset;
import brs.Asset;
import brs.AssetTransfer;
import brs.Trade;
import brs.db.BurstIterator;

public interface AssetService {

  Asset getAsset(long assetId);

  BurstIterator<AccountAsset> getAccounts(long assetId, int from, int to);

  BurstIterator<AccountAsset> getAccounts(long assetId, int height, int from, int to);

  BurstIterator<Trade> getTrades(long assetId, int from, int to);

  BurstIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to);
}
