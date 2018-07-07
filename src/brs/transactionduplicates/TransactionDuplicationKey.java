package brs.transactionduplicates;

import brs.TransactionType;

public class TransactionDuplicationKey {

  final TransactionType transactionType;

  final String key;

  public static TransactionDuplicationKey IS_ALWAYS_DUPLICATE = new TransactionDuplicationKey(null, "always");

  public static TransactionDuplicationKey IS_NEVER_DUPLICATE = new TransactionDuplicationKey(null, "never");

  public TransactionDuplicationKey(TransactionType transactionType, String key) {
    this.transactionType = transactionType;
    this.key = key;
  }
}
