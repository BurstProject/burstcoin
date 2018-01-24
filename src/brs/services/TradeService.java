package brs.services;

import brs.Trade;
import brs.db.BurstIterator;

public interface TradeService {

  BurstIterator<Trade> getAssetTrades(long assetId, int from, int to);

  BurstIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

  BurstIterator<Trade> getAccountTrades(long id, int from, int to);

  int getCount();

  int getTradeCount(long assetId);

  BurstIterator<Trade> getAllTrades(int from, int to);
}
