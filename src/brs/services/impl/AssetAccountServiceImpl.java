package brs.services.impl;

import brs.Account.AccountAsset;
import brs.Burst;
import brs.db.BurstIterator;
import brs.db.store.AccountStore;
import brs.services.AssetAccountService;

public class AssetAccountServiceImpl implements AssetAccountService {

  private final AccountStore accountStore;

  public AssetAccountServiceImpl(AccountStore accountStore) {
    this.accountStore = accountStore;
  }

  @Override
  public BurstIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
    return accountStore.getAssetAccounts(assetId, from, to);
  }

  @Override
  public BurstIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
    if (height < 0) {
      return getAssetAccounts(assetId, from, to);
    }
    return accountStore.getAssetAccounts(assetId, height, from, to);
  }

  @Override
  public int getAssetAccountsCount(long assetId) {
    return accountStore.getAssetAccountsCount(assetId);
  }
}
