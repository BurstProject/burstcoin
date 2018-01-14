package brs.services.impl;

import brs.Trade;
import brs.db.BurstIterator;
import brs.db.store.TradeStore;
import brs.services.TradeService;

public class TradeServiceImpl implements TradeService {

  private TradeStore tradeStore;

  public TradeServiceImpl(TradeStore tradeStore) {
    this.tradeStore = tradeStore;
  }

  @Override
  public BurstIterator<Trade> getAssetTrades(long assetId, int from, int to) {
    return tradeStore.getAssetTrades(assetId, from, to);
  }

}
