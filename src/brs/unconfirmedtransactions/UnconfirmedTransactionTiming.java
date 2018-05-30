package brs.unconfirmedtransactions;

class UnconfirmedTransactionTiming {

  private long id;
  private long timestamp;

  public UnconfirmedTransactionTiming(long id, long timestamp) {
    this.id = id;
    this.timestamp = timestamp;
  }

  public long getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
