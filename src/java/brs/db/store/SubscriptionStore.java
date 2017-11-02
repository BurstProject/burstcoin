package brs.db.store;

import brs.Subscription;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.VersionedEntityTable;

public interface SubscriptionStore {

    NxtKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory();

    VersionedEntityTable<Subscription> getSubscriptionTable();

    NxtIterator<Subscription> getSubscriptionsByParticipant(Long accountId);

    NxtIterator<Subscription> getIdSubscriptions(Long accountId);

    NxtIterator<Subscription> getSubscriptionsToId(Long accountId);

    NxtIterator<Subscription> getUpdateSubscriptions(int timestamp);
}
