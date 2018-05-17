package brs.assetexchange;

import brs.Account.AccountAsset;
import brs.Asset;
import brs.AssetTransfer;
import brs.Attachment;
import brs.Trade;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.sql.EntitySqlTable;
import brs.db.store.AssetStore;

class AssetServiceImpl {

  private final AssetStore assetStore;
  private final AssetAccountServiceImpl assetAccountService;
  private final TradeServiceImpl tradeService;
  private final AssetTransferServiceImpl assetTransferService;

  private final EntitySqlTable<Asset> assetTable;

  private final BurstKey.LongKeyFactory<Asset> assetDbKeyFactory;

  public AssetServiceImpl(AssetAccountServiceImpl assetAccountService, TradeServiceImpl tradeService, AssetStore assetStore, AssetTransferServiceImpl assetTransferService) {
    this.assetAccountService = assetAccountService;
    this.tradeService = tradeService;
    this.assetStore = assetStore;
    this.assetTable = assetStore.getAssetTable();
    this.assetDbKeyFactory = assetStore.getAssetDbKeyFactory();
    this.assetTransferService = assetTransferService;
  }

  public Asset getAsset(long id) {
    return assetTable.get(assetDbKeyFactory.newKey(id));
  }

  public BurstIterator<AccountAsset> getAccounts(long assetId, int from, int to) {
    return assetAccountService.getAssetAccounts(assetId, from, to);
  }

  public BurstIterator<AccountAsset> getAccounts(long assetId, int height, int from, int to) {
    if (height < 0) {
      return getAccounts(assetId, from, to);
    }
    return assetAccountService.getAssetAccounts(assetId, height, from, to);
  }

  public BurstIterator<Trade> getTrades(long assetId, int from, int to) {
    return tradeService.getAssetTrades(assetId, from, to);
  }

  public BurstIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
    return assetTransferService.getAssetTransfers(assetId, from, to);
  }

  public BurstIterator<Asset> getAllAssets(int from, int to) {
    return assetTable.getAll(from, to);
  }

  public BurstIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
    return assetStore.getAssetsIssuedBy(accountId, from, to);
  }

  public int getAssetsCount() {
    return assetTable.getCount();
  }

  public void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
    final BurstKey dbKey = assetDbKeyFactory.newKey(transaction.getId());
    assetTable.insert(new Asset(dbKey, transaction, attachment));
  }

}
