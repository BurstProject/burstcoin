package brs.db.store;

import brs.Account;
import brs.db.BurstIterator;
import brs.db.VersionedBatchEntityTable;
import brs.db.VersionedEntityTable;
import brs.db.BurstKey;

/**
 * Interface for Database operations related to Accounts
 * Created by BraindeadOne on 10.08.2017.
 */
public interface AccountStore {

    VersionedBatchEntityTable<Account> getAccountTable();

    VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable();

    BurstKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentKeyFactory();

    BurstKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetKeyFactory();

    VersionedEntityTable<Account.AccountAsset> getAccountAssetTable();

    int getAssetAccountsCount(long assetId);

    BurstKey.LongKeyFactory<Account> getAccountKeyFactory();

    BurstIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);

    BurstIterator<Account.AccountAsset> getAssets(int from, int to, Long id);

    BurstIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to);

    BurstIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to);
    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    boolean setOrVerify(Account acc, byte[] key, int height);
}
