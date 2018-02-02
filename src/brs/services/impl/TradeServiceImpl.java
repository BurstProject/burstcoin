package brs.services.impl;

import brs.Block;
import brs.Order;
import brs.Trade;
import brs.Trade.Event;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.BurstKey.LinkKeyFactory;
import brs.db.sql.EntitySqlTable;
import brs.db.store.TradeStore;
import brs.services.TradeService;
import brs.util.Listener;
import brs.util.Listeners;

public class TradeServiceImpl implements TradeService {

  private final Listeners<Trade,Event> listeners = new Listeners<>();

  private final TradeStore tradeStore;
  private final EntitySqlTable<Trade> tradeTable;
  private final LinkKeyFactory<Trade> tradeDbKeyFactory;


  public TradeServiceImpl(TradeStore tradeStore) {
    this.tradeStore = tradeStore;
    this.tradeTable = tradeStore.getTradeTable();
    this.tradeDbKeyFactory = tradeStore.getTradeDbKeyFactory();
  }

  @Override
  public BurstIterator<Trade> getAssetTrades(long assetId, int from, int to) {
    return tradeStore.getAssetTrades(assetId, from, to);
  }

  @Override
  public BurstIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to) {
    return tradeStore.getAccountAssetTrades(accountId, assetId, from, to);
  }

  @Override
  public BurstIterator<Trade> getAccountTrades(long id, int from, int to) {
    return tradeStore.getAccountTrades(id, from, to);
  }

  @Override
  public int getCount() {
    return tradeTable.getCount();
  }

  @Override
  public int getTradeCount(long assetId) {
    return tradeStore.getTradeCount(assetId);
  }

  @Override
  public BurstIterator<Trade> getAllTrades(int from, int to) {
    return tradeTable.getAll(from, to);
  }

  @Override
  public boolean addListener(Listener<Trade> listener, Event eventType) {
    return listeners.addListener(listener, eventType);
  }

  @Override
  public boolean removeListener(Listener<Trade> listener, Event eventType) {
    return listeners.removeListener(listener, eventType);
  }

  @Override
  public Trade addTrade(long assetId, Block block, Order.Ask askOrder, Order.Bid bidOrder) {
    BurstKey dbKey = tradeDbKeyFactory.newKey(askOrder.getId(), bidOrder.getId());
    Trade trade = new Trade(dbKey, assetId, block, askOrder, bidOrder);
    tradeTable.insert(trade);
    listeners.notify(trade, Event.TRADE);
    return trade;
  }
}
