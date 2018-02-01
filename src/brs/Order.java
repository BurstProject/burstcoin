package brs;

import brs.db.BurstKey;
import brs.util.Convert;

public abstract class Order {

  private final long id;
  private final long accountId;
  private final long assetId;
  private final long priceNQT;
  private final int creationHeight;

  private long quantityQNT;

  private Order(Transaction transaction, Attachment.ColoredCoinsOrderPlacement attachment) {
    this.id = transaction.getId();
    this.accountId = transaction.getSenderId();
    this.assetId = attachment.getAssetId();
    this.quantityQNT = attachment.getQuantityQNT();
    this.priceNQT = attachment.getPriceNQT();
    this.creationHeight = transaction.getHeight();
  }

  protected Order(long id, long accountId, long assetId, long priceNQT, int creationHeight, long quantityQNT) {
    this.id = id;
    this.accountId = accountId;
    this.assetId = assetId;
    this.priceNQT = priceNQT;
    this.creationHeight = creationHeight;
    this.quantityQNT = quantityQNT;
  }

  public long getId() {
    return id;
  }

  public long getAccountId() {
    return accountId;
  }

  public long getAssetId() {
    return assetId;
  }

  public long getPriceNQT() {
    return priceNQT;
  }

  public long getQuantityQNT() {
    return quantityQNT;
  }

  public int getHeight() {
    return creationHeight;
  }

  public void setQuantityQNT(long quantityQNT) {
    this.quantityQNT = quantityQNT;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " id: " + Convert.toUnsignedLong(id) + " account: " + Convert.toUnsignedLong(accountId)
        + " asset: " + Convert.toUnsignedLong(assetId) + " price: " + priceNQT + " quantity: " + quantityQNT + " height: " + creationHeight;
  }

  public static class Ask extends Order {

    public final BurstKey dbKey;

    public Ask(BurstKey dbKey, Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment) {
      super(transaction, attachment);
      this.dbKey = dbKey;
    }

    public Ask(long id, long accountId, long assetId, long priceNQT, int creationHeight, long quantityQNT, BurstKey dbKey) {
      super(id, accountId, assetId, priceNQT, creationHeight, quantityQNT);
      this.dbKey = dbKey;
    }

  }

  public static class Bid extends Order {

    public final BurstKey dbKey;

    public Bid(BurstKey dbKey, Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment) {
      super(transaction, attachment);
      this.dbKey = dbKey;
    }

    public Bid(long id, long accountId, long assetId, long priceNQT, int creationHeight, long quantityQNT, BurstKey dbKey) {
      super(id, accountId, assetId, priceNQT, creationHeight, quantityQNT);
      this.dbKey = dbKey;
    }
  }
}
