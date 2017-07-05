package nxt;

import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.db.Db;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;
import nxt.db.VersionedBatchEntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public final class Account {

    public static enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE,
        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED
    }

    public static class AccountAsset {

        private final long accountId;
        private final long assetId;
        private final DbKey dbKey;
        private long quantityQNT;
        private long unconfirmedQuantityQNT;

        private AccountAsset(long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
            this.quantityQNT = quantityQNT;
            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
        }

        private AccountAsset(ResultSet rs) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.assetId = rs.getLong("asset_id");
            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
            this.quantityQNT = rs.getLong("quantity");
            this.unconfirmedQuantityQNT = rs.getLong("unconfirmed_quantity");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
                    + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
                    + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setLong(++i, this.assetId);
                pstmt.setLong(++i, this.quantityQNT);
                pstmt.setLong(++i, this.unconfirmedQuantityQNT);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getUnconfirmedQuantityQNT() {
            return unconfirmedQuantityQNT;
        }

        private void save() {
            checkBalance(this.accountId, this.quantityQNT, this.unconfirmedQuantityQNT);
            if (this.quantityQNT > 0 || this.unconfirmedQuantityQNT > 0) {
                accountAssetTable.insert(this);
            } else {
                accountAssetTable.delete(this);
            }
        }

        @Override
        public String toString() {
            return "AccountAsset account_id: " + Convert.toUnsignedLong(accountId) + " asset_id: " + Convert.toUnsignedLong(assetId)
                    + " quantity: " + quantityQNT + " unconfirmedQuantity: " + unconfirmedQuantityQNT;
        }

    }

    public static class AccountLease {

        public final long lessorId;
        public final long lesseeId;
        public final int fromHeight;
        public final int toHeight;

        private AccountLease(long lessorId, long lesseeId, int fromHeight, int toHeight) {
            this.lessorId = lessorId;
            this.lesseeId = lesseeId;
            this.fromHeight = fromHeight;
            this.toHeight = toHeight;
        }

    }
    
    public static class RewardRecipientAssignment {
    	
    	public final Long accountId;
    	private Long prevRecipientId;
    	private Long recipientId;
    	private int fromHeight;
    	private final DbKey dbKey;
    	
    	private RewardRecipientAssignment(Long accountId, Long prevRecipientId, Long recipientId, int fromHeight) {
    		this.accountId = accountId;
    		this.prevRecipientId = prevRecipientId;
    		this.recipientId = recipientId;
    		this.fromHeight = fromHeight;
    		this.dbKey = rewardRecipientAssignmentDbKeyFactory.newKey(this.accountId);
    	}
    	
    	private RewardRecipientAssignment(ResultSet rs) throws SQLException {
    		this.accountId = rs.getLong("account_id");
    		this.dbKey = rewardRecipientAssignmentDbKeyFactory.newKey(this.accountId);
    		this.prevRecipientId = rs.getLong("prev_recip_id");
    		this.recipientId = rs.getLong("recip_id");
    		this.fromHeight = (int) rs.getLong("from_height");
    	}
    	
    	private void save(Connection con) throws SQLException {
    		try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO reward_recip_assign "
    				+ "(account_id, prev_recip_id, recip_id, from_height, height, latest) KEY (account_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
    			int i = 0;
    			pstmt.setLong(++i, this.accountId);
    			pstmt.setLong(++i, this.prevRecipientId);
    			pstmt.setLong(++i, this.recipientId);
    			pstmt.setInt(++i, this.fromHeight);
    			pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
    			pstmt.executeUpdate();
    		}
    	}
    	
    	public long getAccountId() {
    		return accountId;
    	}
    	
    	public long getPrevRecipientId() {
    		return prevRecipientId;
    	}
    	
    	public long getRecipientId() {
    		return recipientId;
    	}
    	
    	public int getFromHeight() {
    		return fromHeight;
    	}
    	
    	public void setRecipient(long newRecipientId, int fromHeight) {
    		prevRecipientId = recipientId;
    		recipientId = newRecipientId;
    		this.fromHeight = fromHeight;
    	}
    }

    static class DoubleSpendingException extends RuntimeException {

        DoubleSpendingException(String message) {
            super(message);
        }

    }

    static {
    }

    private static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {

        @Override
        public DbKey newKey(Account account) {
            return account.dbKey;
        }

    };

    private static final VersionedBatchEntityDbTable<Account> accountTable = new VersionedBatchEntityDbTable<Account>("account", accountDbKeyFactory) {

        @Override
        protected Account load(Connection con, ResultSet rs) throws SQLException {
            return new Account(rs);
        }

        /*@Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }*/

        @Override
        protected String updateQuery() {
            return "MERGE INTO account (id, creation_height, public_key, key_height, balance, unconfirmed_balance, " +
                    "forged_balance, name, description, height, latest) " +
                    "KEY (id, height) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
        }

        @Override
        protected void batch(PreparedStatement pstmt, Account account) throws SQLException {
            account.batch(pstmt);
        }

    };

    public static void flushAccountTable() {
        accountTable.finish();
    }

    private static final DbKey.LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new DbKey.LinkKeyFactory<AccountAsset>("account_id", "asset_id") {

        @Override
        public DbKey newKey(AccountAsset accountAsset) {
            return accountAsset.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountAsset> accountAssetTable = new VersionedEntityDbTable<AccountAsset>("account_asset", accountAssetDbKeyFactory) {

        @Override
        protected AccountAsset load(Connection con, ResultSet rs) throws SQLException {
            return new AccountAsset(rs);
        }

        @Override
        protected void save(Connection con, AccountAsset accountAsset) throws SQLException {
            accountAsset.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY quantity DESC, account_id, asset_id ";
        }

    };
    
    private static final DbKey.LongKeyFactory<RewardRecipientAssignment> rewardRecipientAssignmentDbKeyFactory = new DbKey.LongKeyFactory<RewardRecipientAssignment>("account_id") {
    	
    	@Override
    	public DbKey newKey(RewardRecipientAssignment assignment) {
    		return assignment.dbKey;
    	}
	};
	
	private static final VersionedEntityDbTable<RewardRecipientAssignment> rewardRecipientAssignmentTable = new VersionedEntityDbTable<RewardRecipientAssignment>("reward_recip_assign", rewardRecipientAssignmentDbKeyFactory) {
		
		@Override
		protected RewardRecipientAssignment load(Connection con, ResultSet rs) throws SQLException {
			return new RewardRecipientAssignment(rs);
		}
		
		@Override
		protected void save(Connection con, RewardRecipientAssignment assignment) throws SQLException {
			assignment.save(con);
		}
	};

    private static final Listeners<Account,Event> listeners = new Listeners<>();

    private static final Listeners<AccountAsset,Event> assetListeners = new Listeners<>();

    public static boolean addListener(Listener<Account> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Account> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static boolean addAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.addListener(listener, eventType);
    }

    public static boolean removeAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.removeListener(listener, eventType);
    }

    public static DbIterator<Account> getAllAccounts(int from, int to) {
        return accountTable.getAll(from, to);
    }

    public static int getCount() {
        return accountTable.getCount();
    }

    public static int getAssetAccountsCount(long assetId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM account_asset WHERE asset_id = ? AND latest = TRUE")) {
            pstmt.setLong(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Account getAccount(long id) {
        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id));
    }
    
    public static Account getAccount(long id, int height) {
        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id), height);
    }

    public static Account getAccount(byte[] publicKey) {
        Account account = accountTable.get(accountDbKeyFactory.newKey(getId(publicKey)));
        if (account == null) {
            return null;
        }
        if (account.getPublicKey() == null || Arrays.equals(account.getPublicKey(), publicKey)) {
            return account;
        }
        throw new RuntimeException("DUPLICATE KEY for account " + Convert.toUnsignedLong(account.getId())
                + " existing key " + Convert.toHexString(account.getPublicKey()) + " new key " + Convert.toHexString(publicKey));
    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    static Account addOrGetAccount(long id) {
        Account account = accountTable.get(accountDbKeyFactory.newKey(id));
        if (account == null) {
            account = new Account(id);
            accountTable.insert(account);
        }
        return account;
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        if (height < 0) {
            return getAssetAccounts(assetId, from, to);
        }
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id ");
    }

    static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final int creationHeight;
    private byte[] publicKey;
    private int keyHeight;
    private long balanceNQT;
    private long unconfirmedBalanceNQT;
    private long forgedBalanceNQT;

    private String name;
    private String description;

    private Account(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            Logger.logMessage("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.dbKey = accountDbKeyFactory.newKey(this.id);
        this.creationHeight = Nxt.getBlockchain().getHeight();
    }

    private Account(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = accountDbKeyFactory.newKey(this.id);
        this.creationHeight = rs.getInt("creation_height");
        this.publicKey = rs.getBytes("public_key");
        this.keyHeight = rs.getInt("key_height");
        this.balanceNQT = rs.getLong("balance");
        this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
        this.forgedBalanceNQT = rs.getLong("forged_balance");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
    }

    /*private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, creation_height, public_key, "
                + "key_height, balance, unconfirmed_balance, forged_balance, name, description, "
                + "height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setInt(++i, this.getCreationHeight());
            DbUtils.setBytes(pstmt, ++i, this.getPublicKey());
            pstmt.setInt(++i, this.getKeyHeight());
            pstmt.setLong(++i, this.getBalanceNQT());
            pstmt.setLong(++i, this.getUnconfirmedBalanceNQT());
            pstmt.setLong(++i, this.getForgedBalanceNQT());
            DbUtils.setString(pstmt, ++i, this.getName());
            DbUtils.setString(pstmt, ++i, this.getDescription());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }*/

    private void batch(PreparedStatement pstmt) throws SQLException {
        int i = 0;
        pstmt.setLong(++i, this.getId());
        pstmt.setInt(++i, this.getCreationHeight());
        DbUtils.setBytes(pstmt, ++i, this.getPublicKey());
        pstmt.setInt(++i, this.getKeyHeight());
        pstmt.setLong(++i, this.getBalanceNQT());
        pstmt.setLong(++i, this.getUnconfirmedBalanceNQT());
        pstmt.setLong(++i, this.getForgedBalanceNQT());
        DbUtils.setString(pstmt, ++i, this.getName());
        DbUtils.setString(pstmt, ++i, this.getDescription());
        pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
        pstmt.addBatch();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    void setAccountInfo(String name, String description) {
        this.name = Convert.emptyToNull(name.trim());
        this.description = Convert.emptyToNull(description.trim());
        accountTable.insert(this);
    }

    public byte[] getPublicKey() {
        if (this.keyHeight == -1) {
            return null;
        }
        return publicKey;
    }

    private int getCreationHeight() {
        return creationHeight;
    }

    private int getKeyHeight() {
        return keyHeight;
    }

    public EncryptedData encryptTo(byte[] data, String senderSecretPhrase) {
        if (getPublicKey() == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return EncryptedData.encrypt(data, Crypto.getPrivateKey(senderSecretPhrase), publicKey);
    }

    public byte[] decryptFrom(EncryptedData encryptedData, String recipientSecretPhrase) {
        if (getPublicKey() == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return encryptedData.decrypt(Crypto.getPrivateKey(recipientSecretPhrase), publicKey);
    }

    public long getBalanceNQT() {
        return balanceNQT;
    }

    public long getUnconfirmedBalanceNQT() {
        return unconfirmedBalanceNQT;
    }

    public long getForgedBalanceNQT() {
        return forgedBalanceNQT;
    }

    public DbIterator<AccountAsset> getAssets(int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<Trade> getTrades(int from, int to) {
        return Trade.getAccountTrades(this.id, from, to);
    }

    public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
        return AssetTransfer.getAccountAssetTransfers(this.id, from, to);
    }

    public long getAssetBalanceQNT(long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        return accountAsset == null ? 0 : accountAsset.quantityQNT;
    }

    public long getUnconfirmedAssetBalanceQNT(long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
    }
    
    public RewardRecipientAssignment getRewardRecipientAssignment() {
    	return getRewardRecipientAssignment(id);
    }
    
    public static RewardRecipientAssignment getRewardRecipientAssignment(Long id) {
    	return rewardRecipientAssignmentTable.get(rewardRecipientAssignmentDbKeyFactory.newKey(id));
    }
    
    public void setRewardRecipientAssignment(Long recipient) {
    	setRewardRecipientAssignment(id, recipient);
    }
    
    public static void setRewardRecipientAssignment(Long id, Long recipient) {
    	int currentHeight = Nxt.getBlockchain().getLastBlock().getHeight();
    	RewardRecipientAssignment assignment = getRewardRecipientAssignment(id);
    	if(assignment == null) {
    		assignment = new RewardRecipientAssignment(id, id, recipient, (int) (currentHeight + Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME));
    	}
    	else {
    		assignment.setRecipient(recipient, (int) (currentHeight + Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME));
    	}
    	rewardRecipientAssignmentTable.insert(assignment);
    }
    
    private static DbClause getAccountsWithRewardRecipientClause(final long id, final int height) {
    	return new DbClause(" recip_id = ? AND from_height <= ? ") {
    		@Override
    		public int set(PreparedStatement pstmt, int index) throws SQLException {
    			pstmt.setLong(index++, id);
    			pstmt.setInt(index++, height);
    			return index;
    		}
    	};
    }
    
    public static DbIterator<RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
    	return rewardRecipientAssignmentTable.getManyBy(getAccountsWithRewardRecipientClause(recipientId, Nxt.getBlockchain().getHeight() + 1), 0, -1);
    }

    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    boolean setOrVerify(byte[] key, int height) {
        if (this.publicKey == null) {
        	if (Db.isInTransaction()) {
        		this.publicKey = key;
                this.keyHeight = -1;
                accountTable.insert(this);
        	}
            return true;
        } else if (Arrays.equals(this.publicKey, key)) {
            return true;
        } else if (this.keyHeight == -1) {
            Logger.logMessage("DUPLICATE KEY!!!");
            Logger.logMessage("Account key for " + Convert.toUnsignedLong(id) + " was already set to a different one at the same height "
                    + ", current height is " + height + ", rejecting new key");
            return false;
        } else if (this.keyHeight >= height) {
            Logger.logMessage("DUPLICATE KEY!!!");
            if (Db.isInTransaction()) {
            	Logger.logMessage("Changing key for account " + Convert.toUnsignedLong(id) + " at height " + height
                        + ", was previously set to a different one at height " + keyHeight);
                this.publicKey = key;
                this.keyHeight = height;
                accountTable.insert(this);
            }
            return true;
        }
        Logger.logMessage("DUPLICATE KEY!!!");
        Logger.logMessage("Invalid key for account " + Convert.toUnsignedLong(id) + " at height " + height
                + ", was already set to a different one at height " + keyHeight);
        return false;
    }

    void apply(byte[] key, int height) {
        if (! setOrVerify(key, this.creationHeight)) {
            throw new IllegalStateException("Public key mismatch");
        }
        if (this.publicKey == null) {
            throw new IllegalStateException("Public key has not been set for account " + Convert.toUnsignedLong(id)
                    +" at height " + height + ", key height is " + keyHeight);
        }
        if (this.keyHeight == -1 || this.keyHeight > height) {
            this.keyHeight = height;
            accountTable.insert(this);
        }
    }

    void addToAssetBalanceQNT(long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, 0);
        } else {
            accountAsset.quantityQNT = assetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
    }

    void addToUnconfirmedAssetBalanceQNT(long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.unconfirmedQuantityQNT = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToAssetAndUnconfirmedAssetBalanceQNT(long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, unconfirmedAssetBalance);
        } else {
            accountAsset.quantityQNT = assetBalance;
            accountAsset.unconfirmedQuantityQNT = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
    }

    void addToBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.balanceNQT = Convert.safeAdd(this.balanceNQT, amountNQT);
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        accountTable.insert(this);
        listeners.notify(this, Event.BALANCE);
    }

    void addToUnconfirmedBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.unconfirmedBalanceNQT = Convert.safeAdd(this.unconfirmedBalanceNQT, amountNQT);
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        accountTable.insert(this);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
    }

    void addToBalanceAndUnconfirmedBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.balanceNQT = Convert.safeAdd(this.balanceNQT, amountNQT);
        this.unconfirmedBalanceNQT = Convert.safeAdd(this.unconfirmedBalanceNQT, amountNQT);
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        accountTable.insert(this);
        listeners.notify(this, Event.BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
    }

    void addToForgedBalanceNQT(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.forgedBalanceNQT = Convert.safeAdd(this.forgedBalanceNQT, amountNQT);
        accountTable.insert(this);
    }

    private static void checkBalance(long accountId, long confirmed, long unconfirmed) {
        if (confirmed < 0) {
            throw new DoubleSpendingException("Negative balance or quantity for account " + Convert.toUnsignedLong(accountId));
        }
        if (unconfirmed < 0) {
            throw new DoubleSpendingException("Negative unconfirmed balance or quantity for account " + Convert.toUnsignedLong(accountId));
        }
        if (unconfirmed > confirmed) {
            throw new DoubleSpendingException("Unconfirmed exceeds confirmed balance or quantity for account " + Convert.toUnsignedLong(accountId));
        }
    }

}
