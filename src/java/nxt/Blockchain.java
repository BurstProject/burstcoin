package nxt;

import nxt.util.DbIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface Blockchain {

    Block getLastBlock();

    int getHeight();

    Block getBlock(Long blockId);

    Block getBlockAtHeight(int height);

    boolean hasBlock(Long blockId);

    DbIterator<? extends Block> getAllBlocks();

    DbIterator<? extends Block> getBlocks(Account account, int timestamp);

    DbIterator<? extends Block> getBlocks(Connection con, PreparedStatement pstmt);

    List<Long> getBlockIdsAfter(Long blockId, int limit);

    List<? extends Block> getBlocksAfter(Long blockId, int limit);

    long getBlockIdAtHeight(int height);

    List<? extends Block> getBlocksFromHeight(int height);

    Transaction getTransaction(Long transactionId);

    Transaction getTransactionByFullHash(String fullHash);

    boolean hasTransaction(Long transactionId);

    boolean hasTransactionByFullHash(String fullHash);

    int getTransactionCount();

    DbIterator<? extends Transaction> getAllTransactions();

    DbIterator<? extends Transaction> getTransactions(Account account, byte type, byte subtype, int blockTimestamp);

    DbIterator<? extends Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, int from, int to);

    DbIterator<? extends Transaction> getTransactions(Connection con, PreparedStatement pstmt);

}
