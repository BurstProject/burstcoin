package brs.services;

import brs.Account;
import brs.Subscription;
import brs.db.BurstIterator;

public interface SubscriptionService {

  Subscription getSubscription(Long id);

  BurstIterator<Subscription> getSubscriptionsByParticipant(Long accountId);

  BurstIterator<Subscription> getSubscriptionsToId(Long accountId);

  void addSubscription(Account sender, Account recipient, Long id, Long amountNQT, int startTimestamp, int frequency);
}
