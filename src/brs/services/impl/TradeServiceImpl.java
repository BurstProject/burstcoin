package brs.services.impl;

import brs.Trade;
import brs.db.BurstIterator;
import brs.db.sql.EntitySqlTable;
import brs.db.store.TradeStore;
import brs.services.TradeService;

public class TradeServiceImpl implements TradeService {

  private final TradeStore tradeStore;
  private final EntitySqlTable<Trade> tradeTable;

  public TradeServiceImpl(TradeStore tradeStore) {
    this.tradeStore = tradeStore;
    this.tradeTable = tradeStore.getTradeTable();
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
}
