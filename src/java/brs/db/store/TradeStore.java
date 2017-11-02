package brs.db.store;

import brs.Trade;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.sql.EntitySqlTable;

public interface TradeStore {
    NxtIterator<Trade> getAllTrades(int from, int to);

    NxtIterator<Trade> getAssetTrades(long assetId, int from, int to);

    NxtIterator<Trade> getAccountTrades(long accountId, int from, int to);

    NxtIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

    int getTradeCount(long assetId);

    NxtKey.LinkKeyFactory<Trade> getTradeDbKeyFactory();

    EntitySqlTable<Trade> getTradeTable();
}
