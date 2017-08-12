package nxt.db.store;

import nxt.Account;
import nxt.db.VersionedBatchEntityTable;

/**
 * Base-Class for Database operations related to Accounts
 * Created by BraindeadOne on 10.08.2017.
 */
public abstract class AccountStore {
//    public abstract void saveAccountAsset(Account.AccountAsset accountAsset);

//    public abstract void saveRewardRecipientAssignment(Account.RewardRecipientAssignment rewardRecipientAssignment);

    public abstract VersionedBatchEntityTable<Account> getAccountTable();

}
