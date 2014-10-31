package nxt;

import nxt.db.DbIterator;

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

    DbIterator<? extends Block> getAllBlocks();
    
    DbIterator<? extends Block> getBlocks(int from, int to);

    DbIterator<? extends Block> getBlocks(Account account, int timestamp);
    
    DbIterator<? extends Block> getBlocks(Account account, int timestamp, int from, int to);

    DbIterator<? extends Block> getBlocks(Connection con, PreparedStatement pstmt);

    List<Long> getBlockIdsAfter(long blockId, int limit);

    List<? extends Block> getBlocksAfter(long blockId, int limit);

    long getBlockIdAtHeight(int height);

    Transaction getTransaction(long transactionId);

    Transaction getTransactionByFullHash(String fullHash);

    boolean hasTransaction(long transactionId);

    boolean hasTransactionByFullHash(String fullHash);

    int getTransactionCount();

    DbIterator<? extends Transaction> getAllTransactions();

    DbIterator<? extends Transaction> getTransactions(Account account, byte type, byte subtype, int blockTimestamp);

    DbIterator<? extends Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, int from, int to);

    DbIterator<? extends Transaction> getTransactions(Connection con, PreparedStatement pstmt);

}
