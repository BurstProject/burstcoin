package nxt;

import nxt.db.NxtIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface Blockchain {

    Block getLastBlock();
    
    Block getLastBlock(int timestamp);

    int getHeight();

    Block getBlock(long blockId);

    Block getBlockAtHeight(int height);

    boolean hasBlock(long blockId);

    NxtIterator<BlockImpl> getAllBlocks();
    
    NxtIterator<BlockImpl> getBlocks(int from, int to);

    NxtIterator<BlockImpl> getBlocks(Account account, int timestamp);
    
    NxtIterator<BlockImpl> getBlocks(Account account, int timestamp, int from, int to);

//    NxtIterator<BlockImpl> getBlocks(Connection con, PreparedStatement pstmt);

    List<Long> getBlockIdsAfter(long blockId, int limit);

    List<? extends Block> getBlocksAfter(long blockId, int limit);

    long getBlockIdAtHeight(int height);

    Transaction getTransaction(long transactionId);

    Transaction getTransactionByFullHash(String fullHash);

    boolean hasTransaction(long transactionId);

    boolean hasTransactionByFullHash(String fullHash);

    int getTransactionCount();

    NxtIterator<TransactionImpl> getAllTransactions();

    NxtIterator<TransactionImpl> getTransactions(Account account, byte type, byte subtype, int blockTimestamp);

    NxtIterator<TransactionImpl> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, int from, int to);

//    NxtIterator<TransactionImpl> getTransactions(Connection con, PreparedStatement pstmt);

}
