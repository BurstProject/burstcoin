package brs.unconfirmedtransactions;

import brs.Transaction;
import java.util.List;

public class TimedUnconfirmedTransactionOverview {

  private final long timestamp;
  private final List<Transaction> transactions;

  public TimedUnconfirmedTransactionOverview(long timestamp, List<Transaction> transactions) {
    this.timestamp = timestamp;
    this.transactions = transactions;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public List<Transaction> getTransactions() {
    return transactions;
  }
}
