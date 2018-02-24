package brs.db.store;

import brs.Account;
import brs.Block;
import brs.Transaction;
import brs.db.BurstIterator;

import java.util.List;
import java.sql.ResultSet;

import org.jooq.DSLContext;

/**
 * Store for both BlockchainImpl and BlockchainProcessorImpl
 */

public interface BlockchainStore {


  BurstIterator<Block> getBlocks(int from, int to);

  BurstIterator<Block> getBlocks(Account account, int timestamp, int from, int to);

  BurstIterator<Block> getBlocks(DSLContext ctx, ResultSet rs);

  List<Long> getBlockIdsAfter(long blockId, int limit);

  List<Block> getBlocksAfter(long blockId, int limit);

  int getTransactionCount();

  BurstIterator<Transaction> getAllTransactions();

  BurstIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                 int blockTimestamp, int from, int to);

  BurstIterator<Transaction> getTransactions(DSLContext ctx, ResultSet rs);

  boolean addBlock(Block block);

  void scan(int height);
}
