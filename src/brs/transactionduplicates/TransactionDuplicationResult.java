package brs.transactionduplicates;

import brs.Transaction;

public class TransactionDuplicationResult {

  boolean duplicate;

  Transaction transaction;

  public TransactionDuplicationResult(boolean duplicate, Transaction transaction) {
    this.duplicate = duplicate;
    this.transaction = transaction;
  }

  public boolean isDuplicate() {
    return duplicate;
  }

  public Transaction getTransaction() {
    return transaction;
  }
}
