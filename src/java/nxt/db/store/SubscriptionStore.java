package nxt.db.store;

import nxt.Subscription;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;

public interface SubscriptionStore {

    NxtKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory();

    VersionedEntityTable<Subscription> getSubscriptionTable();

    NxtIterator<Subscription> getSubscriptionsByParticipant(Long accountId);

    NxtIterator<Subscription> getIdSubscriptions(Long accountId);

    NxtIterator<Subscription> getSubscriptionsToId(Long accountId);

    NxtIterator<Subscription> getUpdateSubscriptions(int timestamp);
}
