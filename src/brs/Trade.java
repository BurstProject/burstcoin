package brs;

import brs.db.EntityTable;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Listeners;


public class Trade {

  public enum Event {
    TRADE
  }

  private static final Listeners<Trade,Event> listeners = new Listeners<>();

  private static final BurstKey.LinkKeyFactory<Trade> tradeDbKeyFactory =
      Burst.getStores().getTradeStore().getTradeDbKeyFactory();

  private static final EntityTable<Trade> tradeTable = Burst.getStores().getTradeStore().getTradeTable();


  public static BurstIterator<Trade> getAllTrades(int from, int to) {
    return tradeTable.getAll(from, to);
  }

  public static int getCount() {
    return tradeTable.getCount();
  }

  public static boolean addListener(Listener<Trade> listener, Event eventType) {
    return listeners.addListener(listener, eventType);
  }

  public static boolean removeListener(Listener<Trade> listener, Event eventType) {
    return listeners.removeListener(listener, eventType);
  }

  public static BurstIterator<Trade> getAssetTrades(long assetId, int from, int to) {
    return Burst.getStores().getTradeStore().getAssetTrades(assetId, from, to);
  }

  public static BurstIterator<Trade> getAccountTrades(long accountId, int from, int to) {
    return Burst.getStores().getTradeStore().getAssetTrades(accountId, from, to);
  }

  public static BurstIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to) {
    return Burst.getStores().getTradeStore().getAccountAssetTrades(accountId, assetId, from, to);

  }

  public static int getTradeCount(long assetId) {
    return Burst.getStores().getTradeStore().getTradeCount(assetId);
  }

  static Trade addTrade(long assetId, Block block, Order.Ask askOrder, Order.Bid bidOrder) {
    Trade trade = new Trade(assetId, block, askOrder, bidOrder);
    tradeTable.insert(trade);
    listeners.notify(trade, Event.TRADE);
    return trade;
  }

  static void init() {}


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

  private Trade(long assetId, Block block, Order.Ask askOrder, Order.Bid bidOrder) {
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
    this.dbKey =  tradeDbKeyFactory.newKey(this.askOrderId, this.bidOrderId);
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
