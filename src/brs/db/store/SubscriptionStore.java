package brs.db.store;

import brs.Subscription;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;

public interface SubscriptionStore {

    BurstKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory();

    VersionedEntityTable<Subscription> getSubscriptionTable();

    BurstIterator<Subscription> getSubscriptionsByParticipant(Long accountId);

    BurstIterator<Subscription> getIdSubscriptions(Long accountId);

    BurstIterator<Subscription> getSubscriptionsToId(Long accountId);

    BurstIterator<Subscription> getUpdateSubscriptions(int timestamp);
}
