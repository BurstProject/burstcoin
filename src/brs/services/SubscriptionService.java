package brs.services;

import brs.Account;
import brs.Block;
import brs.Subscription;
import brs.db.BurstIterator;

public interface SubscriptionService {

  Subscription getSubscription(Long id);

  BurstIterator<Subscription> getSubscriptionsByParticipant(Long accountId);

  BurstIterator<Subscription> getSubscriptionsToId(Long accountId);

  void addSubscription(Account sender, Account recipient, Long id, Long amountNQT, int startTimestamp, int frequency);

  boolean isEnabled();

  void applyConfirmed(Block block, int blockchainHeight);

  void removeSubscription(Long id);

  long calculateFees(int timestamp);

  void clearRemovals();

  void addRemoval(Long id);

  long applyUnconfirmed(int timestamp);
}
