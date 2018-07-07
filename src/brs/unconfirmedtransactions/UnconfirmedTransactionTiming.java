package brs.unconfirmedtransactions;

import brs.Transaction;

class UnconfirmedTransactionTiming {

  private final Transaction transaction;
  private final long timestamp;

  public UnconfirmedTransactionTiming(Transaction transaction, long timestamp) {
    this.transaction = transaction;
    this.timestamp = timestamp;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
