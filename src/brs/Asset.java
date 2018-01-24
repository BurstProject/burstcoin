package brs;

import brs.db.EntityTable;
import brs.db.BurstKey;

public class Asset {

  private static final BurstKey.LongKeyFactory<Asset> assetDbKeyFactory() {
    return Burst.getStores().getAssetStore().getAssetDbKeyFactory();
  }

  private static final EntityTable<Asset> assetTable() {
    return Burst.getStores().getAssetStore().getAssetTable();
  }

  public static int getCount() {
    return assetTable().getCount();
  }

  public static Asset getAsset(long id) {
    return assetTable().get(assetDbKeyFactory().newKey(id));
  }

  static void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
    assetTable().insert(new Asset(transaction, attachment));
  }

  static void init() {
  }


  private final long assetId;
  public final BurstKey dbKey;
  private final long accountId;
  private final String name;
  private final String description;
  private final long quantityQNT;
  private final byte decimals;

  protected Asset(long assetId, BurstKey dbKey, long accountId, String name, String description, long quantityQNT, byte decimals) {
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
    this.dbKey = assetDbKeyFactory().newKey(this.assetId);
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

}
