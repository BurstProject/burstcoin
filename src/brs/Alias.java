package brs;

import brs.db.BurstIterator;
import brs.db.VersionedEntityTable;
import brs.db.BurstKey;

public class Alias {

  public static class Offer {

    private long priceNQT;
    private long buyerId;
    private final long aliasId;
    public final BurstKey dbKey;

    protected Offer(long aliasId, long priceNQT, long buyerId) {
      this.priceNQT = priceNQT;
      this.buyerId = buyerId;
      this.aliasId = aliasId;
      this.dbKey = offerDbKeyFactory().newKey(this.aliasId);
    }

    protected Offer(long aliasId, long priceNQT, long buyerId, BurstKey nxtKey) {
      this.priceNQT = priceNQT;
      this.buyerId = buyerId;
      this.aliasId = aliasId;
      this.dbKey = nxtKey;
    }

    public long getId() {
      return aliasId;
    }

    public long getPriceNQT() {
      return priceNQT;
    }

    public long getBuyerId() {
      return buyerId;
    }

  }

  private static final BurstKey.LongKeyFactory<Alias> aliasDbKeyFactory() {
    return Burst.getStores().getAliasStore().getAliasDbKeyFactory();
  }

  private static final VersionedEntityTable<Alias> aliasTable() {
    return Burst.getStores().getAliasStore().getAliasTable();
  }

  private static final BurstKey.LongKeyFactory<Offer> offerDbKeyFactory() {
    return Burst.getStores().getAliasStore().getOfferDbKeyFactory();
  }

  private static final VersionedEntityTable<Offer> offerTable() {
    return Burst.getStores().getAliasStore().getOfferTable();
  }

  public static int getCount() {
    return aliasTable().getCount();
  }

  public static BurstIterator<Alias> getAliasesByOwner(long accountId, int from, int to) {
    return Burst.getStores().getAliasStore().getAliasesByOwner(accountId, from, to);
  }

  public static Alias getAlias(String aliasName) {
    return Burst.getStores().getAliasStore().getAlias(aliasName);
  }

  public static Alias getAlias(long id) {
    return aliasTable().get(aliasDbKeyFactory().newKey(id));
  }

  public static Offer getOffer(Alias alias) {
    return offerTable().get(offerDbKeyFactory().newKey(alias.getId()));
  }

  static void addOrUpdateAlias(Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
    Alias alias = getAlias(attachment.getAliasName());
    if (alias == null) {
      alias = new Alias(transaction.getId(), transaction, attachment);
    }
    else {
      alias.accountId = transaction.getSenderId();
      alias.aliasURI = attachment.getAliasURI();
      alias.timestamp = transaction.getBlockTimestamp();
    }
    aliasTable().insert(alias);
  }

  static void sellAlias(Transaction transaction, Attachment.MessagingAliasSell attachment) {
    final String aliasName = attachment.getAliasName();
    final long priceNQT = attachment.getPriceNQT();
    final long buyerId = transaction.getRecipientId();
    if (priceNQT > 0) {
      Alias alias = getAlias(aliasName);
      Offer offer = getOffer(alias);
      if (offer == null) {
        offerTable().insert(new Offer(alias.id, priceNQT, buyerId));
      }
      else {
        offer.priceNQT = priceNQT;
        offer.buyerId = buyerId;
        offerTable().insert(offer);
      }
    }
    else {
      changeOwner(buyerId, aliasName, transaction.getBlockTimestamp());
    }

  }

  static void changeOwner(long newOwnerId, String aliasName, int timestamp) {
    Alias alias = getAlias(aliasName);
    alias.accountId = newOwnerId;
    alias.timestamp = timestamp;
    aliasTable().insert(alias);
    Offer offer = getOffer(alias);
    offerTable().delete(offer);
  }

  static void init() {
  }


  private long accountId;
  private final long id;
  public final BurstKey dbKey;
  private final String aliasName;
  private String aliasURI;
  private int timestamp;

  private Alias(long id, long accountId, String aliasName, String aliasURI, int timestamp) {
    this.id = id;
    this.dbKey = aliasDbKeyFactory().newKey(this.id);
    this.accountId = accountId;
    this.aliasName = aliasName;
    this.aliasURI = aliasURI;
    this.timestamp = timestamp;
  }

  protected Alias(long id, long accountId, String aliasName, String aliasURI, int timestamp, BurstKey dbKey) {
    this.id = id;
    this.dbKey = dbKey;
    this.accountId = accountId;
    this.aliasName = aliasName;
    this.aliasURI = aliasURI;
    this.timestamp = timestamp;
  }


  private Alias(long aliasId, Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
    this(aliasId, transaction.getSenderId(), attachment.getAliasName(), attachment.getAliasURI(),
         transaction.getBlockTimestamp());
  }


  public long getId() {
    return id;
  }

  public String getAliasName() {
    return aliasName;
  }

  public String getAliasURI() {
    return aliasURI;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public long getAccountId() {
    return accountId;
  }

}
