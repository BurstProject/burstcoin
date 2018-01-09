package brs.services.impl;

import brs.Account;
import brs.Account.RewardRecipientAssignment;
import brs.AssetTransfer;
import brs.Burst;
import brs.crypto.Crypto;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedBatchEntityTable;
import brs.services.AccountService;
import brs.util.Convert;
import java.util.Arrays;

public class AccountServiceImpl implements AccountService {

  private final VersionedBatchEntityTable<Account> accountTable;
  private final LongKeyFactory<Account> accountBurstKeyFactory;

  public AccountServiceImpl(VersionedBatchEntityTable<Account> accountTable, BurstKey.LongKeyFactory<Account> accountBurstKeyFactory) {
    this.accountTable = accountTable;
    this.accountBurstKeyFactory = accountBurstKeyFactory;
  }

  @Override
  public Account getAccount(long id) {
    return id == 0 ? null : accountTable.get(accountBurstKeyFactory.newKey(id));
  }

  @Override
  public Account getAccount(long id, int height) {
    return id == 0 ? null : accountTable.get(accountBurstKeyFactory.newKey(id), height);
  }

  @Override
  public Account getAccount(byte[] publicKey) {
    Account account = accountTable.get(accountBurstKeyFactory.newKey(getId(publicKey)));
    if (account == null) {
      return null;
    }
    if (account.getPublicKey() == null || Arrays.equals(account.getPublicKey(), publicKey)) {
      return account;
    }
    throw new RuntimeException("DUPLICATE KEY for account " + Convert.toUnsignedLong(account.getId())
        + " existing key " + Convert.toHexString(account.getPublicKey()) + " new key " + Convert.toHexString(publicKey));
  }

  @Override
  public BurstIterator<AssetTransfer> getAssetTransfers(long accountId, int from, int to) {
    return AssetTransfer.getAccountAssetTransfers(accountId, from, to);
  }

  @Override
  public BurstIterator<RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
    return Burst.getStores().getAccountStore().getAccountsWithRewardRecipient(recipientId);
  }

  public static long getId(byte[] publicKey) {
    byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
    return Convert.fullHashToId(publicKeyHash);
  }
}
