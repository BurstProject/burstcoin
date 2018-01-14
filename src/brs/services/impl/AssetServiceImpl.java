package brs.services.impl;

import brs.Account.AccountAsset;
import brs.Asset;
import brs.AssetTransfer;
import brs.Trade;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.sql.EntitySqlTable;
import brs.db.store.AssetStore;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.TradeService;

public class AssetServiceImpl implements AssetService {

  private final AssetAccountService assetAccountService;
  private final TradeService tradeService;
  private final AssetTransferService assetTransferService;

  private final EntitySqlTable<Asset> assetTable;

  private final BurstKey.LongKeyFactory<Asset> assetDbKeyFactory;

  public AssetServiceImpl(AssetAccountService assetAccountService, TradeService tradeService, AssetStore assetStore, AssetTransferService assetTransferService) {
    this.assetAccountService = assetAccountService;
    this.tradeService = tradeService;
    this.assetTable = assetStore.getAssetTable();
    this.assetDbKeyFactory = assetStore.getAssetDbKeyFactory();
    this.assetTransferService = assetTransferService;
  }

  @Override
  public Asset getAsset(long id) {
    return assetTable.get(assetDbKeyFactory.newKey(id));
  }

  @Override
  public BurstIterator<AccountAsset> getAccounts(long assetId, int from, int to) {
    return assetAccountService.getAssetAccounts(assetId, from, to);
  }

  @Override
  public BurstIterator<AccountAsset> getAccounts(long assetId, int height, int from, int to) {
    if (height < 0) {
      return getAccounts(assetId, from, to);
    }
    return assetAccountService.getAssetAccounts(assetId, height, from, to);
  }

  @Override
  public BurstIterator<Trade> getTrades(long assetId, int from, int to) {
    return tradeService.getAssetTrades(assetId, from, to);
  }

  @Override
  public BurstIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
    return assetTransferService.getAssetTransfers(assetId, from, to);
  }

}
