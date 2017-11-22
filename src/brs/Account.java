package brs;

import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.db.BurstIterator;
import brs.db.VersionedBatchEntityTable;
import brs.db.VersionedEntityTable;

import brs.db.BurstKey;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Listeners;

import java.util.Arrays;
import java.util.logging.Logger;

public  class Account {

  private static final Logger logger = Logger.getLogger(Account.class.getSimpleName());

  public enum Event {
    BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE,
    LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED
  }

  public static class AccountAsset {

    public final long accountId;
    public final long assetId;
    public final BurstKey nxtKey;
    public long quantityQNT;
    public long unconfirmedQuantityQNT;

    protected AccountAsset(long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT, BurstKey BurstKey) {
      this.accountId = accountId;
      this.assetId = assetId;
      this.quantityQNT = quantityQNT;
      this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
      this.nxtKey = BurstKey;
    }

    protected AccountAsset(long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT) {
      this.accountId = accountId;
      this.assetId = assetId;
      this.nxtKey = Burst.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(this.accountId, this.assetId);
      this.quantityQNT = quantityQNT;
      this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
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
      return "AccountAsset account_id: "
          + Convert.toUnsignedLong(accountId)
          + " asset_id: "
          + Convert.toUnsignedLong(assetId)
          + " quantity: "
          + quantityQNT
          + " unconfirmedQuantity: "
          + unconfirmedQuantityQNT;
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
    public Long prevRecipientId;
    public Long recipientId;
    public int fromHeight;
    public final BurstKey nxtKey;


    protected RewardRecipientAssignment(Long accountId, Long prevRecipientId, Long recipientId, int fromHeight, BurstKey BurstKey) {
      this.accountId = accountId;
      this.prevRecipientId = prevRecipientId;
      this.recipientId = recipientId;
      this.fromHeight = fromHeight;
      this.nxtKey = BurstKey;
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

  protected static final BurstKey.LongKeyFactory<Account> accountBurstKeyFactory =  Burst.getStores().getAccountStore().getAccountKeyFactory();

  private static final VersionedBatchEntityTable<Account> accountTable = Burst.getStores().getAccountStore().getAccountTable();


  public static void flushAccountTable() {
    accountTable.finish();
  }



  private static final VersionedEntityTable<AccountAsset> accountAssetTable =  Burst.getStores().getAccountStore().getAccountAssetTable();


  private static final VersionedEntityTable<RewardRecipientAssignment> rewardRecipientAssignmentTable = Burst.getStores().getAccountStore().getRewardRecipientAssignmentTable();

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

  public static BurstIterator<Account> getAllAccounts(int from, int to) {
    return accountTable.getAll(from, to);
  }

  public static int getCount() {
    return accountTable.getCount();
  }

  public static int getAssetAccountsCount(long assetId) {
    return Burst.getStores().getAccountStore().getAssetAccountsCount(assetId);
  }

  public static Account getAccount(long id) {
    return id == 0 ? null : accountTable.get(accountBurstKeyFactory.newKey(id));
  }

  public static Account getAccount(long id, int height) {
    return id == 0 ? null : accountTable.get(accountBurstKeyFactory.newKey(id), height);
  }

  public static Account getAccount(byte[] publicKey) {
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

  public static long getId(byte[] publicKey) {
    byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
    return Convert.fullHashToId(publicKeyHash);
  }

  static Account addOrGetAccount(long id) {
    Account account = accountTable.get(accountBurstKeyFactory.newKey(id));
    if (account == null) {
      account = new Account(id);
      accountTable.insert(account);
    }
    return account;
  }

  public static BurstIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
    return Burst.getStores().getAccountStore().getAssetAccounts(assetId, from, to);
  }

  public static BurstIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
    if (height < 0) {
      return getAssetAccounts(assetId, from, to);
    }
    return Burst.getStores().getAccountStore().getAssetAccounts(assetId, height, from, to);
  }

  static void init() {}


  public final long id;
  public final BurstKey nxtKey;
  protected final int creationHeight;
  public byte[] publicKey;
  public int keyHeight;
  protected long balanceNQT;
  protected long unconfirmedBalanceNQT;
  protected long forgedBalanceNQT;

  protected String name;
  protected String description;


  protected Account(long id) {
    if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
      logger.info("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
    }
    this.id = id;
    this.nxtKey = accountBurstKeyFactory.newKey(this.id);
    this.creationHeight = Burst.getBlockchain().getHeight();
  }

  protected Account(long id, BurstKey BurstKey, int creationHeight) {
    if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
      logger.info("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
    }
    this.id = id;
    this.nxtKey = BurstKey;
    this.creationHeight = creationHeight;
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

  public int getCreationHeight() {
    return creationHeight;
  }

  public int getKeyHeight() {
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

  public BurstIterator<AccountAsset> getAssets(int from, int to) {
    return Burst.getStores().getAccountStore().getAssets(from, to, this.id);
  }

  public BurstIterator<Trade> getTrades(int from, int to) {
    return Trade.getAccountTrades(this.id, from, to);
  }

  public BurstIterator<AssetTransfer> getAssetTransfers(int from, int to) {
    return AssetTransfer.getAccountAssetTransfers(this.id, from, to);
  }

  public long getAssetBalanceQNT(long assetId) {
    BurstKey nxtKey =  Burst.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(this.id, assetId);
    AccountAsset accountAsset = accountAssetTable.get(nxtKey);
    return accountAsset == null ? 0 : accountAsset.quantityQNT;
  }

  public long getUnconfirmedAssetBalanceQNT(long assetId) {
    BurstKey nxtKey =  Burst.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(this.id, assetId);
    AccountAsset accountAsset = accountAssetTable.get(nxtKey);
    return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
  }

  public RewardRecipientAssignment getRewardRecipientAssignment() {
    return getRewardRecipientAssignment(id);
  }

  public static RewardRecipientAssignment getRewardRecipientAssignment(Long id) {
    return rewardRecipientAssignmentTable.get(
                                              Burst.getStores().getAccountStore().getRewardRecipientAssignmentKeyFactory().newKey(id)
                                              );
  }

  public void setRewardRecipientAssignment(Long recipient) {
    setRewardRecipientAssignment(id, recipient);
  }

  public static void setRewardRecipientAssignment(Long id, Long recipient) {
    int currentHeight = Burst.getBlockchain().getLastBlock().getHeight();
    RewardRecipientAssignment assignment = getRewardRecipientAssignment(id);
    if(assignment == null) {
      BurstKey BurstKey =     Burst.getStores().getAccountStore().getRewardRecipientAssignmentKeyFactory().newKey(id);
      assignment = new RewardRecipientAssignment(id, id, recipient, (int) (currentHeight + Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME), BurstKey);
    }
    else {
      assignment.setRecipient(recipient, (int) (currentHeight + Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME));
    }
    rewardRecipientAssignmentTable.insert(assignment);
  }



  public static BurstIterator<RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
    return Burst.getStores().getAccountStore().getAccountsWithRewardRecipient(recipientId);
  }

  // returns true iff:
  // this.publicKey is set to null (in which case this.publicKey also gets set to key)
  // or
  // this.publicKey is already set to an array equal to key
  boolean setOrVerify(byte[] key, int height) {
    return Burst.getStores().getAccountStore().setOrVerify(this, key, height);
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

    BurstKey nxtKey =  Burst.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(this.id, assetId);
    accountAsset = accountAssetTable.get(nxtKey);
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
    BurstKey nxtKey =  Burst.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(this.id, assetId);
    accountAsset = accountAssetTable.get(nxtKey);
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
    BurstKey nxtKey =  Burst.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(this.id, assetId);
    accountAsset = accountAssetTable.get(nxtKey);
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
