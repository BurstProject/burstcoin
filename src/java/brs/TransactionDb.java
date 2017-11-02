package brs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

public interface TransactionDb {
  Transaction findTransaction(long transactionId);

  Transaction findTransactionByFullHash(String fullHash);

  boolean hasTransaction(long transactionId);

  boolean hasTransactionByFullHash(String fullHash);

  TransactionImpl loadTransaction(Connection con, ResultSet rs) throws BurstException.ValidationException;

  List<TransactionImpl> findBlockTransactions(long blockId);

  void saveTransactions(List<TransactionImpl> transactions);

}
