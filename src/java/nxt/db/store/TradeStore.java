package nxt.db.store;

import nxt.Trade;
import nxt.db.sql.DbIterator;
import nxt.db.sql.DbKey;
import nxt.db.sql.EntitySqlTable;

public interface TradeStore {
    DbIterator<Trade> getAllTrades(int from, int to);

    DbIterator<Trade> getAssetTrades(long assetId, int from, int to);

    DbIterator<Trade> getAccountTrades(long accountId, int from, int to);

    DbIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

    int getTradeCount(long assetId);

    DbKey.LinkKeyFactory<Trade> getTradeDbKeyFactory();

    EntitySqlTable<Trade> getTradeTable();
}
