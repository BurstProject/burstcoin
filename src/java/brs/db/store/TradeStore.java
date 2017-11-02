package brs.db.store;

import brs.Trade;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.sql.EntitySqlTable;

public interface TradeStore {
    BurstIterator<Trade> getAllTrades(int from, int to);

    BurstIterator<Trade> getAssetTrades(long assetId, int from, int to);

    BurstIterator<Trade> getAccountTrades(long accountId, int from, int to);

    BurstIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

    int getTradeCount(long assetId);

    BurstKey.LinkKeyFactory<Trade> getTradeDbKeyFactory();

    EntitySqlTable<Trade> getTradeTable();
}
