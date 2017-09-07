package nxt.db.store;

import nxt.Account;
import nxt.BlockImpl;
import nxt.TransactionImpl;
import nxt.db.NxtIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Store for both BlockchainImpl and BlockchainProcessorImpl
 */

public interface BlockchainStore {


    NxtIterator<BlockImpl> getAllBlocks();

    NxtIterator<BlockImpl> getBlocks(int from, int to);

    NxtIterator<BlockImpl> getBlocks(Account account, int timestamp, int from, int to);

    NxtIterator<BlockImpl> getBlocks(Connection con, PreparedStatement pstmt);

    List<Long> getBlockIdsAfter(long blockId, int limit);

    List<BlockImpl> getBlocksAfter(long blockId, int limit);

    int getTransactionCount();

    NxtIterator<TransactionImpl> getAllTransactions();

    NxtIterator<TransactionImpl> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                 int blockTimestamp, int from, int to);

    NxtIterator<TransactionImpl> getTransactions(Connection con, PreparedStatement pstmt);

    boolean addBlock(BlockImpl block);

    void scan(int height);
}
