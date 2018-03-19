package brs;

import brs.db.BurstKey;

public class Alias {

  private long accountId;
  private final long id;
  public final BurstKey dbKey;
  private final String aliasName;
  private String aliasURI;
  private int timestamp;

  private Alias(BurstKey dbKey, long id, long accountId, String aliasName, String aliasURI, int timestamp) {
    this.id = id;
    this.dbKey = dbKey;
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

  public Alias(long aliasId, BurstKey dbKey, Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
    this(dbKey, aliasId, transaction.getSenderId(), attachment.getAliasName(), attachment.getAliasURI(),
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

  public void setAccountId(long accountId) {
    this.accountId = accountId;
  }

  public void setAliasURI(String aliasURI) {
    this.aliasURI = aliasURI;
  }

  public void setTimestamp(int timestamp) {
    this.timestamp = timestamp;
  }

  public static class Offer {

    private long priceNQT;
    private long buyerId;
    private final long aliasId;
    public final BurstKey dbKey;

    public Offer(BurstKey dbKey, long aliasId, long priceNQT, long buyerId) {
      this.dbKey = dbKey;
      this.priceNQT = priceNQT;
      this.buyerId = buyerId;
      this.aliasId = aliasId;
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

    public void setPriceNQT(long priceNQT) {
      this.priceNQT = priceNQT;
    }

    public void setBuyerId(long buyerId) {
      this.buyerId = buyerId;
    }
  }

}
