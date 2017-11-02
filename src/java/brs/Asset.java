package brs;

import brs.db.EntityTable;
import brs.db.NxtIterator;
import brs.db.NxtKey;

public class Asset {

  private static final NxtKey.LongKeyFactory<Asset> assetDbKeyFactory = Nxt.getStores().getAssetStore().getAssetDbKeyFactory();

  private static final EntityTable<Asset> assetTable = Nxt.getStores().getAssetStore().getAssetTable();

  public static NxtIterator<Asset> getAllAssets(int from, int to) {
    return assetTable.getAll(from, to);
  }

  public static int getCount() {
    return assetTable.getCount();
  }

  public static Asset getAsset(long id) {
    return assetTable.get(assetDbKeyFactory.newKey(id));
  }

  public static NxtIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
    return Nxt.getStores().getAssetStore().getAssetsIssuedBy(accountId, from, to);
  }

  static void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
    assetTable.insert(new Asset(transaction, attachment));
  }

  static void init() {
  }


  private final long assetId;
  public final NxtKey dbKey;
  private final long accountId;
  private final String name;
  private final String description;
  private final long quantityQNT;
  private final byte decimals;

  protected Asset(long assetId, NxtKey dbKey, long accountId, String name, String description, long quantityQNT, byte decimals) {
    this.assetId = assetId;
    this.dbKey = dbKey;
    this.accountId = accountId;
    this.name = name;
    this.description = description;
    this.quantityQNT = quantityQNT;
    this.decimals = decimals;
  }

  private Asset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
    this.assetId = transaction.getId();
    this.dbKey = assetDbKeyFactory.newKey(this.assetId);
    this.accountId = transaction.getSenderId();
    this.name = attachment.getName();
    this.description = attachment.getDescription();
    this.quantityQNT = attachment.getQuantityQNT();
    this.decimals = attachment.getDecimals();
  }

  public long getId() {
    return assetId;
  }

  public long getAccountId() {
    return accountId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public long getQuantityQNT() {
    return quantityQNT;
  }

  public byte getDecimals() {
    return decimals;
  }

  public NxtIterator<Account.AccountAsset> getAccounts(int from, int to) {
    return Account.getAssetAccounts(this.assetId, from, to);
  }

  public NxtIterator<Account.AccountAsset> getAccounts(int height, int from, int to) {
    if (height < 0) {
      return getAccounts(from, to);
    }
    return Account.getAssetAccounts(this.assetId, height, from, to);
  }

  public NxtIterator<Trade> getTrades(int from, int to) {
    return Trade.getAssetTrades(this.assetId, from, to);
  }

  public NxtIterator<AssetTransfer> getAssetTransfers(int from, int to) {
    return AssetTransfer.getAssetTransfers(this.assetId, from, to);
  }
}
