package brs.services;

import brs.Account;
import brs.Account.AccountAsset;
import brs.Account.RewardRecipientAssignment;
import brs.AssetTransfer;
import brs.db.BurstIterator;

public interface AccountService {

  Account getAccount(long id);

  Account getAccount(long id, int height);

  Account getAccount(byte[] publicKey);

  BurstIterator<AssetTransfer> getAssetTransfers(long accountId, int from, int to);

  BurstIterator<AccountAsset> getAssets(long accountId, int from, int to);

  BurstIterator<RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);

  BurstIterator<Account> getAllAccounts(int from, int to);

  Account addOrGetAccount(long id);

  void flushAccountTable();

}
