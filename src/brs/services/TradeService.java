package brs.services;

import brs.Block;
import brs.Order;
import brs.Trade;
import brs.Trade.Event;
import brs.db.BurstIterator;
import brs.util.Listener;

public interface TradeService {

  BurstIterator<Trade> getAssetTrades(long assetId, int from, int to);

  BurstIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

  BurstIterator<Trade> getAccountTrades(long id, int from, int to);

  int getCount();

  int getTradeCount(long assetId);

  BurstIterator<Trade> getAllTrades(int from, int to);

  /*
    private static final BurstKey.LinkKeyFactory<Trade> tradeDbKeyFactory() {
      return Burst.getStores().getTradeStore().getTradeDbKeyFactory();
    }

    private static final EntityTable<Trade> tradeTable() {
      return Burst.getStores().getTradeStore().getTradeTable();
    }
  */
  boolean addListener(Listener<Trade> listener, Event eventType);

  boolean removeListener(Listener<Trade> listener, Event eventType);

  Trade addTrade(long assetId, Block block, Order.Ask askOrder, Order.Bid bidOrder);
}
