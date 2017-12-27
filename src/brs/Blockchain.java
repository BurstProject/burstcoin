package brs;

import brs.db.BurstIterator;

import java.util.List;

public interface Blockchain {

  Block getLastBlock();
    
  Block getLastBlock(int timestamp);

  void setLastBlock(BlockImpl block);

  int getHeight();

  Block getBlock(long blockId);

  Block getBlockAtHeight(int height);

  boolean hasBlock(long blockId);

  BurstIterator<BlockImpl> getBlocks(int from, int to);

  BurstIterator<BlockImpl> getBlocks(Account account, int timestamp);
    
  BurstIterator<BlockImpl> getBlocks(Account account, int timestamp, int from, int to);

  List<Long> getBlockIdsAfter(long blockId, int limit);

  List<? extends Block> getBlocksAfter(long blockId, int limit);

  long getBlockIdAtHeight(int height);

  Transaction getTransaction(long transactionId);

  Transaction getTransactionByFullHash(String fullHash);

  boolean hasTransaction(long transactionId);

  boolean hasTransactionByFullHash(String fullHash);

  int getTransactionCount();

  BurstIterator<TransactionImpl> getAllTransactions();

  BurstIterator<TransactionImpl> getTransactions(Account account, byte type, byte subtype, int blockTimestamp);

  BurstIterator<TransactionImpl> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, int from, int to);

}
