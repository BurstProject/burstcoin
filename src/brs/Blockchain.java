package brs;

import brs.db.BurstIterator;

import java.util.List;

public interface Blockchain {

  Block getLastBlock();
    
  Block getLastBlock(int timestamp);

  void setLastBlock(Block BlockImpl);

  int getHeight();

  Block getBlock(long BlockImplId);

  Block getBlockAtHeight(int height);

  boolean hasBlock(long BlockImplId);

  BurstIterator<Block> getBlocks(int from, int to);

  BurstIterator<Block> getBlocks(Account account, int timestamp);
    
  BurstIterator<Block> getBlocks(Account account, int timestamp, int from, int to);

  List<Long> getBlockIdsAfter(long BlockImplId, int limit);

  List<? extends Block> getBlocksAfter(long BlockImplId, int limit);

  long getBlockIdAtHeight(int height);

  Transaction getTransaction(long transactionId);

  Transaction getTransactionByFullHash(String fullHash);

  boolean hasTransaction(long transactionId);

  boolean hasTransactionByFullHash(String fullHash);

  int getTransactionCount();

  BurstIterator<Transaction> getAllTransactions();

  BurstIterator<Transaction> getTransactions(Account account, byte type, byte subtype, int BlockImplTimestamp);

  BurstIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype, int BlockImplTimestamp, int from, int to);

}
