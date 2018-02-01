package brs;

import brs.db.EntityTable;
import brs.db.BurstKey;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Listeners;


public class Trade {

  public enum Event {
    TRADE
  }

  private final int timestamp;
  private final long assetId;
  private final long blockId;
  private final int height;
  private final long askOrderId;
  private final long bidOrderId;
  private final int askOrderHeight;
  private final int bidOrderHeight;
  private final long sellerId;
  private final long buyerId;
  public final BurstKey dbKey;
  private final long quantityQNT;
  private final long priceNQT;
  private final boolean isBuy;

  public Trade(int timestamp, long assetId, long blockId, int height,
               long askOrderId, long bidOrderId, int askOrderHeight, int bidOrderHeight,
               long sellerId, long buyerId, BurstKey dbKey, long quantityQNT, long priceNQT) {
    this.timestamp = timestamp;
    this.assetId = assetId;
    this.blockId = blockId;
    this.height = height;
    this.askOrderId = askOrderId;
    this.bidOrderId = bidOrderId;
    this.askOrderHeight = askOrderHeight;
    this.bidOrderHeight = bidOrderHeight;
    this.sellerId = sellerId;
    this.buyerId = buyerId;
    this.dbKey = dbKey;
    this.quantityQNT = quantityQNT;
    this.priceNQT = priceNQT;
    this.isBuy = askOrderHeight < bidOrderHeight || (askOrderHeight == bidOrderHeight && askOrderId < bidOrderId);
  }

  public Trade(BurstKey dbKey, long assetId, Block block, Order.Ask askOrder, Order.Bid bidOrder) {
    this.dbKey = dbKey;
    this.blockId = block.getId();
    this.height = block.getHeight();
    this.assetId = assetId;
    this.timestamp = block.getTimestamp();
    this.askOrderId = askOrder.getId();
    this.bidOrderId = bidOrder.getId();
    this.askOrderHeight = askOrder.getHeight();
    this.bidOrderHeight = bidOrder.getHeight();
    this.sellerId = askOrder.getAccountId();
    this.buyerId = bidOrder.getAccountId();
    this.quantityQNT = Math.min(askOrder.getQuantityQNT(), bidOrder.getQuantityQNT());
    this.isBuy = askOrderHeight < bidOrderHeight || (askOrderHeight == bidOrderHeight && askOrderId < bidOrderId);
    this.priceNQT = isBuy ? askOrder.getPriceNQT() : bidOrder.getPriceNQT();
  }

  public long getBlockId() { return blockId; }

  public long getAskOrderId() { return askOrderId; }

  public long getBidOrderId() { return bidOrderId; }

  public int getAskOrderHeight() {
    return askOrderHeight;
  }

  public int getBidOrderHeight() {
    return bidOrderHeight;
  }

  public long getSellerId() {
    return sellerId;
  }

  public long getBuyerId() {
    return buyerId;
  }

  public long getQuantityQNT() { return quantityQNT; }

  public long getPriceNQT() { return priceNQT; }

  public long getAssetId() { return assetId; }

  public int getTimestamp() { return timestamp; }

  public int getHeight() {
    return height;
  }

  public boolean isBuy() {
    return isBuy;
  }

  @Override
  public String toString() {
    return "Trade asset: " + Convert.toUnsignedLong(assetId) + " ask: " + Convert.toUnsignedLong(askOrderId)
        + " bid: " + Convert.toUnsignedLong(bidOrderId) + " price: " + priceNQT + " quantity: " + quantityQNT + " height: " + height;
  }

}
