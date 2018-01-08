package brs.services;

import brs.Account;
import brs.Account.RewardRecipientAssignment;
import brs.db.BurstIterator;

public interface AccountService {

  abstract Account getAccount(long id);

  Account getAccount(long id, int height);

  Account getAccount(byte[] publicKey);

  BurstIterator<RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);
}
