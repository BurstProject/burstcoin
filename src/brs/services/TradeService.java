package brs.services;

import brs.Trade;
import brs.db.BurstIterator;

public interface TradeService {

  BurstIterator<Trade> getAssetTrades(long assetId, int from, int to);
}
