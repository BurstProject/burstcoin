package brs.db.store;

import brs.Order;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.VersionedEntityTable;
import brs.db.sql.DbKey;

public interface OrderStore {
    VersionedEntityTable<Order.Bid> getBidOrderTable();

    NxtIterator<Order.Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    NxtIterator<Order.Ask> getSortedAsks(long assetId, int from, int to);

    Order.Ask getNextOrder(long assetId);

    NxtIterator<Order.Ask> getAll(int from, int to);

    NxtIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to);

    NxtIterator<Order.Ask> getAskOrdersByAsset(long assetId, int from, int to);

    NxtKey.LongKeyFactory<Order.Ask> getAskOrderDbKeyFactory();

    VersionedEntityTable<Order.Ask> getAskOrderTable();

    NxtKey.LongKeyFactory<Order.Bid> getBidOrderDbKeyFactory();

    NxtIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to);

    NxtIterator<Order.Bid> getBidOrdersByAsset(long assetId, int from, int to);

    NxtIterator<Order.Bid> getBidOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    NxtIterator<Order.Bid> getSortedBids(long assetId, int from, int to);

    Order.Bid getNextBid(long assetId);
}
