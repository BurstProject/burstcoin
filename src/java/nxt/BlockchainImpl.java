package nxt;

import nxt.db.BlockDb;
import nxt.db.NxtIterator;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

final class BlockchainImpl implements Blockchain {

    private static final BlockchainImpl instance = new BlockchainImpl();
    private final TransactionDb transactionDb = Nxt.getDbs().getTransactionDb();
    private final BlockDb blockDb =  Nxt.getDbs().getBlockDb();;


    static BlockchainImpl getInstance() {
        return instance;
    }

    private BlockchainImpl() {}

    private final AtomicReference<BlockImpl> lastBlock = new AtomicReference<>();

    @Override
    public BlockImpl getLastBlock() {
        return lastBlock.get();
    }

    void setLastBlock(BlockImpl block) {
        lastBlock.set(block);
    }

    void setLastBlock(BlockImpl previousBlock, BlockImpl block) {
        if (! lastBlock.compareAndSet(previousBlock, block)) {
            throw new IllegalStateException("Last block is no longer previous block");
        }
    }

    @Override
    public int getHeight() {
        BlockImpl last = lastBlock.get();
        return last == null ? 0 : last.getHeight();
    }
    
    @Override
    public BlockImpl getLastBlock(int timestamp) {
        BlockImpl block = lastBlock.get();
        if (timestamp >= block.getTimestamp()) {
            return block;
        }
        return blockDb.findLastBlock(timestamp);
    }

    @Override
    public BlockImpl getBlock(long blockId) {
        BlockImpl block = lastBlock.get();
        if (block.getId() == blockId) {
            return block;
        }
        return blockDb.findBlock(blockId);
    }

    @Override
    public boolean hasBlock(long blockId) {
        return lastBlock.get().getId() == blockId || blockDb.hasBlock(blockId);
    }

    @Override
    public NxtIterator<BlockImpl> getAllBlocks() {
        return Nxt.getStores().getBlockchainStore().getAllBlocks();
    }
    
    @Override
    public NxtIterator<BlockImpl> getBlocks(int from, int to) {
        return Nxt.getStores().getBlockchainStore().getBlocks(from, to);
    }

    @Override
    public NxtIterator<BlockImpl> getBlocks(Account account, int timestamp) {
        return getBlocks(account, timestamp, 0, -1);
    }

    @Override
    public NxtIterator<BlockImpl> getBlocks(Account account, int timestamp, int from, int to) {
        return Nxt.getStores().getBlockchainStore().getBlocks(account, timestamp, from, to);
    }

    @Override
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        return Nxt.getStores().getBlockchainStore().getBlockIdsAfter(blockId, limit);
    }

    @Override
    public List<BlockImpl> getBlocksAfter(long blockId, int limit) {
        return Nxt.getStores().getBlockchainStore().getBlocksAfter(blockId, limit);
    }

    @Override
    public long getBlockIdAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block.getId();
        }
        return blockDb.findBlockIdAtHeight(height);
    }

    @Override
    public BlockImpl getBlockAtHeight(int height) {
        BlockImpl block = lastBlock.get();
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
        return Nxt.getStores().getBlockchainStore().getTransactionCount();
    }

    @Override
    public NxtIterator<TransactionImpl> getAllTransactions() {
        return Nxt.getStores().getBlockchainStore().getAllTransactions();
    }

    @Override
    public NxtIterator<TransactionImpl> getTransactions(Account account, byte type, byte subtype, int blockTimestamp) {
        return getTransactions(account, 0, type, subtype, blockTimestamp, 0, -1);
    }

    @Override
    public NxtIterator<TransactionImpl> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                        int blockTimestamp, int from, int to) {
        return Nxt.getStores().getBlockchainStore().getTransactions(account, numberOfConfirmations, type, subtype, blockTimestamp, from, to);

    }


}
