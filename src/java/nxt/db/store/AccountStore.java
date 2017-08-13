package nxt.db.store;

import nxt.Account;
import nxt.Nxt;
import nxt.db.NxtIterator;
import nxt.db.VersionedBatchEntityTable;
import nxt.db.VersionedEntityTable;
import nxt.db.sql.DbClause;
import nxt.db.sql.DbIterator;
import nxt.db.sql.DbKey;

/**
 * Base-Class for Database operations related to Accounts
 * Created by BraindeadOne on 10.08.2017.
 */
public abstract class AccountStore {

    public abstract VersionedBatchEntityTable<Account> getAccountTable();

    public abstract VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable();

    public abstract DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentDbKeyFactory();

    public abstract DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetDbKeyFactory();

    public abstract VersionedEntityTable<Account.AccountAsset> getAccountAssetTable();

    public abstract int getAssetAccountsCount(long assetId);

    public abstract DbKey.LongKeyFactory<Account> getAccountDbKeyFactory();

    public abstract NxtIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);

    public abstract NxtIterator<Account.AccountAsset> getAssets(int from, int to, Long id);

    public abstract NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to);

    public abstract NxtIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to);


}
