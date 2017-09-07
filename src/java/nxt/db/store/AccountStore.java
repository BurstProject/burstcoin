package nxt.db.store;

import nxt.Account;
import nxt.db.NxtIterator;
import nxt.db.VersionedBatchEntityTable;
import nxt.db.VersionedEntityTable;
import nxt.db.NxtKey;

/**
 * Interface for Database operations related to Accounts
 * Created by BraindeadOne on 10.08.2017.
 */
public interface AccountStore {

    VersionedBatchEntityTable<Account> getAccountTable();

    VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable();

    NxtKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentKeyFactory();

    NxtKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetKeyFactory();

    VersionedEntityTable<Account.AccountAsset> getAccountAssetTable();

    int getAssetAccountsCount(long assetId);

    NxtKey.LongKeyFactory<Account> getAccountKeyFactory();

    NxtIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);

    NxtIterator<Account.AccountAsset> getAssets(int from, int to, Long id);

    NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to);

    NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to);
    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    boolean setOrVerify(Account acc, byte[] key, int height);
}
