package brs;

import brs.db.BlockDb;
import brs.db.BurstIterator;

import brs.db.store.BlockchainStore;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;

public class BlockchainImpl implements Blockchain {

  private final TransactionDb transactionDb;
  private final BlockDb blockDb;
  private final BlockchainStore blockchainStore;
  
  private final StampedLock bcsl;
  
  BlockchainImpl(TransactionDb transactionDb, BlockDb blockDb, BlockchainStore blockchainStore) {
    this.transactionDb = transactionDb;
    this.blockDb = blockDb;
    this.blockchainStore = blockchainStore;
    this.bcsl = new StampedLock();
  }

  private final AtomicReference<Block> lastBlock = new AtomicReference<>();

  @Override
  public Block getLastBlock() {
    long stamp = bcsl.tryOptimisticRead();
    Block retBlock= lastBlock.get();
    if (!bcsl.validate(stamp)) {
      stamp = bcsl.readLock();
      try {
        retBlock= lastBlock.get();
      } finally {
        bcsl.unlockRead(stamp);
      }
   }
   return retBlock;
  }

  @Override
  public void setLastBlock(Block block) {
    long stamp = bcsl.writeLock();
    try {
      lastBlock.set(block);
    } finally {
      bcsl.unlockWrite(stamp);
    }
  }

  void setLastBlock(Block previousBlock, Block block) {
    long stamp = bcsl.writeLock();
    try {
      if (! lastBlock.compareAndSet(previousBlock, block)) {
        throw new IllegalStateException("Last block is no longer previous block");
      }
    } finally {
      bcsl.unlockWrite(stamp);
    }
  }

  @Override
  public int getHeight() {
    long stamp = bcsl.tryOptimisticRead();  
    Block last = lastBlock.get();
    if (!bcsl.validate(stamp)) {
      stamp = bcsl.readLock();
      try {
        last = lastBlock.get();
      } finally {
        bcsl.unlockRead(stamp);
      }
    }
    return last == null ? 0 : last.getHeight();
  }
    
  @Override
  public Block getLastBlock(int timestamp) {
    Block block = getSafelastBlock();
    if (timestamp >= block.getTimestamp()) {
      return block;
    }
    return blockDb.findLastBlock(timestamp);
  }

  @Override
  public Block getBlock(long blockId) {
    Block block = getSafelastBlock();
    if (block.getId() == blockId) {
      return block;
    }
    return blockDb.findBlock(blockId);
  }
  
  private Block getSafelastBlock() {
    long stamp = bcsl.tryOptimisticRead();
    Block block = lastBlock.get();
    if (!bcsl.validate(stamp)) {
      stamp = bcsl.readLock();
      try {
        block = lastBlock.get();
      } finally {
        bcsl.unlockRead(stamp);
      }
    }
    return block;
  }

  @Override
  public boolean hasBlock(long blockId) {
    return getSafelastBlock().getId() == blockId || blockDb.hasBlock(blockId);
  }

  @Override
  public BurstIterator<Block> getBlocks(int from, int to) {
    return blockchainStore.getBlocks(from, to);
  }

  @Override
  public BurstIterator<Block> getBlocks(Account account, int timestamp) {
    return getBlocks(account, timestamp, 0, -1);
  }

  @Override
  public BurstIterator<Block> getBlocks(Account account, int timestamp, int from, int to) {
    return blockchainStore.getBlocks(account, timestamp, from, to);
  }

  @Override
  public List<Long> getBlockIdsAfter(long blockId, int limit) {
    return blockchainStore.getBlockIdsAfter(blockId, limit);
  }

  @Override
  public List<Block> getBlocksAfter(long blockId, int limit) {
    return blockchainStore.getBlocksAfter(blockId, limit);
  }

  @Override
  public long getBlockIdAtHeight(int height) {
    Block block = getSafelastBlock();
    if (height > block.getHeight()) {
      throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
    }
    if (height == block.getHeight()) {
      return block.getId();
    }
    return blockDb.findBlockIdAtHeight(height);
  }

  @Override
  public Block getBlockAtHeight(int height) {
    Block block = getSafelastBlock();
    if (height > block.getHeight()) {
      throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
    }
    if (height == block.getHeight()) {
      return block;
    }
    return blockDb.findBlockAtHeight(height);
  }

  @Override
  public Transaction getTransaction(long transactionId) {
    return transactionDb.findTransaction(transactionId);
  }

  @Override
  public Transaction getTransactionByFullHash(String fullHash) {
    return transactionDb.findTransactionByFullHash(fullHash);
  }

  @Override
  public boolean hasTransaction(long transactionId) {
    return transactionDb.hasTransaction(transactionId);
  }

  @Override
  public boolean hasTransactionByFullHash(String fullHash) {
    return transactionDb.hasTransactionByFullHash(fullHash);
  }

  @Override
  public int getTransactionCount() {
    return blockchainStore.getTransactionCount();
  }

  @Override
  public BurstIterator<Transaction> getAllTransactions() {
    return blockchainStore.getAllTransactions();
  }

  @Override
  public BurstIterator<Transaction> getTransactions(Account account, byte type, byte subtype, int blockTimestamp) {
    return getTransactions(account, 0, type, subtype, blockTimestamp, 0, -1);
  }

  @Override
  public BurstIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                      int blockTimestamp, int from, int to) {
    return  blockchainStore.getTransactions(account, numberOfConfirmations, type, subtype, blockTimestamp, from, to);
  }


}
