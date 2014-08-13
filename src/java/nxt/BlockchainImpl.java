package nxt;

import nxt.util.DbIterator;
import nxt.util.DbUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

final class BlockchainImpl implements Blockchain {

    private static final BlockchainImpl instance = new BlockchainImpl();

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
    public BlockImpl getBlock(Long blockId) {
        return BlockDb.findBlock(blockId);
    }

    @Override
    public boolean hasBlock(Long blockId) {
        return BlockDb.hasBlock(blockId);
    }

    @Override
    public DbIterator<BlockImpl> getAllBlocks() {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(Account account, int timestamp) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp >= ? AND generator_id = ? ORDER BY db_id ASC");
            pstmt.setInt(1, timestamp);
            pstmt.setLong(2, account.getId());
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<BlockImpl>() {
            @Override
            public BlockImpl get(Connection con, ResultSet rs) throws NxtException.ValidationException {
                return BlockDb.loadBlock(con, rs);
            }
        });
    }

    @Override
    public List<Long> getBlockIdsAfter(Long blockId, int limit) {
        if (limit > 1440) {
            throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt1 = con.prepareStatement("SELECT db_id FROM block WHERE id = ?");
             PreparedStatement pstmt2 = con.prepareStatement("SELECT id FROM block WHERE db_id > ? ORDER BY db_id ASC LIMIT ?")) {
            pstmt1.setLong(1, blockId);
            ResultSet rs = pstmt1.executeQuery();
            if (! rs.next()) {
                rs.close();
                return Collections.emptyList();
            }
            List<Long> result = new ArrayList<>();
            int dbId = rs.getInt("db_id");
            pstmt2.setInt(1, dbId);
            pstmt2.setInt(2, limit);
            rs = pstmt2.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("id"));
            }
            rs.close();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<BlockImpl> getBlocksAfter(Long blockId, int limit) {
        if (limit > 1440) {
            throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE db_id > (SELECT db_id FROM block WHERE id = ?) ORDER BY db_id ASC LIMIT ?")) {
            List<BlockImpl> result = new ArrayList<>();
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(BlockDb.loadBlock(con, rs));
            }
            rs.close();
            return result;
        } catch (NxtException.ValidationException|SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
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
        return BlockDb.findBlockIdAtHeight(height);
    }

    @Override
    public List<BlockImpl> getBlocksFromHeight(int height) {
        if (height < 0 || lastBlock.get().getHeight() - height > 1440) {
            throw new IllegalArgumentException("Can't go back more than 1440 blocks");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height >= ? ORDER BY height ASC")) {
            pstmt.setInt(1, height);
            ResultSet rs = pstmt.executeQuery();
            List<BlockImpl> result = new ArrayList<>();
            while (rs.next()) {
                result.add(BlockDb.loadBlock(con, rs));
            }
            return result;
        } catch (SQLException|NxtException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Transaction getTransaction(Long transactionId) {
        return TransactionDb.findTransaction(transactionId);
    }

    @Override
    public Transaction getTransactionByFullHash(String fullHash) {
        return TransactionDb.findTransactionByFullHash(fullHash);
    }

    @Override
    public boolean hasTransaction(Long transactionId) {
        return TransactionDb.hasTransaction(transactionId);
    }

    @Override
    public boolean hasTransactionByFullHash(String fullHash) {
        return TransactionDb.hasTransactionByFullHash(fullHash);
    }

    @Override
    public int getTransactionCount() {
        try (Connection con = Db.getConnection(); PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction")) {
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getAllTransactions() {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(Account account, byte type, byte subtype, int timestamp) {
        return getTransactions(account, type, subtype, timestamp, Boolean.TRUE);
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(Account account, byte type, byte subtype, int timestamp, Boolean orderAscending) {
        Connection con = null;
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT * FROM transaction WHERE (recipient_id = ? OR sender_id = ?) ");
            if (timestamp > 0) {
                buf.append("AND timestamp >= ? ");
            }
            if (type >= 0) {
                buf.append("AND type = ? ");
                if (subtype >= 0) {
                    buf.append("AND subtype = ? ");
                }
            }
            if (Boolean.TRUE.equals(orderAscending)) {
                buf.append("ORDER BY timestamp ASC");
            } else if (Boolean.FALSE.equals(orderAscending)) {
                buf.append("ORDER BY timestamp DESC");
            }
            con = Db.getConnection();
            PreparedStatement pstmt;
            int i = 0;
            pstmt = con.prepareStatement(buf.toString());
            pstmt.setLong(++i, account.getId());
            pstmt.setLong(++i, account.getId());
            if (timestamp > 0) {
                pstmt.setInt(++i, timestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<TransactionImpl>() {
            @Override
            public TransactionImpl get(Connection con, ResultSet rs) throws NxtException.ValidationException {
                return TransactionDb.loadTransaction(con, rs);
            }
        });
    }

}
