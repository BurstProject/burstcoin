package brs.db.store;

import brs.Account;
import brs.BlockImpl;
import brs.TransactionImpl;
import brs.db.BurstIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Store for both BlockchainImpl and BlockchainProcessorImpl
 */

public interface BlockchainStore {


  BurstIterator<BlockImpl> getBlocks(int from, int to);

  BurstIterator<BlockImpl> getBlocks(Account account, int timestamp, int from, int to);

  BurstIterator<BlockImpl> getBlocks(Connection con, PreparedStatement pstmt);

  List<Long> getBlockIdsAfter(long blockId, int limit);

  List<BlockImpl> getBlocksAfter(long blockId, int limit);

  int getTransactionCount();

  BurstIterator<TransactionImpl> getAllTransactions();

  BurstIterator<TransactionImpl> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                 int blockTimestamp, int from, int to);

  BurstIterator<TransactionImpl> getTransactions(Connection con, PreparedStatement pstmt);

  boolean addBlock(BlockImpl block);

  void scan(int height);
}
