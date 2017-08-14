package nxt.db.store;

import nxt.Account;
import nxt.db.NxtIterator;
import nxt.db.VersionedBatchEntityTable;
import nxt.db.VersionedEntityTable;
import nxt.db.sql.DbKey;

/**
 * Interface for Database operations related to Accounts
 * Created by BraindeadOne on 10.08.2017.
 */
public interface AccountStore {

    VersionedBatchEntityTable<Account> getAccountTable();

    VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable();

    DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentDbKeyFactory();

    DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetDbKeyFactory();

    VersionedEntityTable<Account.AccountAsset> getAccountAssetTable();

    int getAssetAccountsCount(long assetId);

    DbKey.LongKeyFactory<Account> getAccountDbKeyFactory();

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
