package brs.services;

import brs.Account.AccountAsset;
import brs.db.BurstIterator;

public interface AssetAccountService {

  BurstIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to);

  BurstIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to);

  int getAssetAccountsCount(long assetId);
}
