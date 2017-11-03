package brs.db.store;

import brs.Order;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;

public interface OrderStore {
    VersionedEntityTable<Order.Bid> getBidOrderTable();

    BurstIterator<Order.Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    BurstIterator<Order.Ask> getSortedAsks(long assetId, int from, int to);

    Order.Ask getNextOrder(long assetId);

    BurstIterator<Order.Ask> getAll(int from, int to);

    BurstIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to);

    BurstIterator<Order.Ask> getAskOrdersByAsset(long assetId, int from, int to);

    BurstKey.LongKeyFactory<Order.Ask> getAskOrderDbKeyFactory();

    VersionedEntityTable<Order.Ask> getAskOrderTable();

    BurstKey.LongKeyFactory<Order.Bid> getBidOrderDbKeyFactory();

    BurstIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to);

    BurstIterator<Order.Bid> getBidOrdersByAsset(long assetId, int from, int to);

    BurstIterator<Order.Bid> getBidOrdersByAccountAsset(long accountId, long assetId, int from, int to);

    BurstIterator<Order.Bid> getSortedBids(long assetId, int from, int to);

    Order.Bid getNextBid(long assetId);
}
