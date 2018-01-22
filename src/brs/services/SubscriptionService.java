package brs.services;

import brs.Subscription;
import brs.db.BurstIterator;

public interface SubscriptionService {

  Subscription getSubscription(Long id);

  BurstIterator<Subscription> getSubscriptionsByParticipant(Long accountId);

  BurstIterator<Subscription> getSubscriptionsToId(Long accountId);
}
