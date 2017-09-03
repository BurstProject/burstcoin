package nxt.db.store;

import nxt.Trade;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.sql.DbKey;
import nxt.db.sql.EntitySqlTable;

public interface TradeStore {
    NxtIterator<Trade> getAllTrades(int from, int to);

    NxtIterator<Trade> getAssetTrades(long assetId, int from, int to);

    NxtIterator<Trade> getAccountTrades(long accountId, int from, int to);

    NxtIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

    int getTradeCount(long assetId);

    NxtKey.LinkKeyFactory<Trade> getTradeDbKeyFactory();

    EntitySqlTable<Trade> getTradeTable();
}
