package brs.db.sql;

import brs.BlockImpl;
import brs.Nxt;
import brs.NxtException;
import brs.db.BlockDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SqlBlockDb implements BlockDb {

    private static final Logger logger = LoggerFactory.getLogger(BlockDb.class);

    @Override
    public BlockImpl findBlock(long blockId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block = null;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Block already in database, id = " + blockId + ", does not pass validation!", e);
        }
    }

    @Override
    public boolean hasBlock(long blockId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public long findBlockIdAtHeight(int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public BlockImpl findBlockAtHeight(int height) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                } else {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Block already in database at height " + height + ", does not pass validation!", e);
        }
    }

    @Override
    public BlockImpl findLastBlock() {
        try (Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id DESC" + DbUtils.limitsClause(1) )) {
            BlockImpl block = null;
            DbUtils.setLimits(1, pstmt, 1);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Last block already in database does not pass validation!", e);
        }
    }

    @Override
    public BlockImpl findLastBlock(int timestamp) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp <= ? ORDER BY timestamp DESC" + DbUtils.limitsClause(1) )) {
            pstmt.setInt(1, timestamp);
            DbUtils.setLimits(2, pstmt, 1);
            BlockImpl block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Block already in database at timestamp " + timestamp + " does not pass validation!", e);
        }
    }

    @Override
    public BlockImpl loadBlock(Connection con, ResultSet rs) throws NxtException.ValidationException {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            long previousBlockId = rs.getLong("previous_block_id");
            long totalAmountNQT = rs.getLong("total_amount");
            long totalFeeNQT = rs.getLong("total_fee");
            int payloadLength = rs.getInt("payload_length");
            byte[] generatorPublicKey = rs.getBytes("generator_public_key");
            byte[] previousBlockHash = rs.getBytes("previous_block_hash");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long baseTarget = rs.getLong("base_target");
            long nextBlockId = rs.getLong("next_block_id");
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");

            long id = rs.getLong("id");
            long nonce = rs.getLong("nonce");

            byte[] blockATs = rs.getBytes("ats");

            return new BlockImpl(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                    generatorPublicKey, generationSignature, blockSignature, previousBlockHash,
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, nonce, blockATs);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void saveBlock(Connection con, BlockImpl block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, "
                    + "total_amount, total_fee, payload_length, generator_public_key, previous_block_hash, cumulative_difficulty, "
                    + "base_target, height, generation_signature, block_signature, payload_hash, generator_id, nonce , ats) "
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, block.getId());
                pstmt.setInt(++i, block.getVersion());
                pstmt.setInt(++i, block.getTimestamp());
                DbUtils.setLongZeroToNull(pstmt, ++i, block.getPreviousBlockId());
                pstmt.setLong(++i, block.getTotalAmountNQT());
                pstmt.setLong(++i, block.getTotalFeeNQT());
                pstmt.setInt(++i, block.getPayloadLength());
                pstmt.setBytes(++i, block.getGeneratorPublicKey());
                pstmt.setBytes(++i, block.getPreviousBlockHash());
                pstmt.setBytes(++i, block.getCumulativeDifficulty().toByteArray());
                pstmt.setLong(++i, block.getBaseTarget());
                pstmt.setInt(++i, block.getHeight());
                pstmt.setBytes(++i, block.getGenerationSignature());
                pstmt.setBytes(++i, block.getBlockSignature());
                pstmt.setBytes(++i, block.getPayloadHash());
                pstmt.setLong(++i, block.getGeneratorId());
                pstmt.setLong(++i, block.getNonce());
                DbUtils.setBytes(pstmt, ++i, block.getBlockATs());
                pstmt.executeUpdate();
                Nxt.getDbs().getTransactionDb().saveTransactions(block.getTransactions());
            }
            if (block.getPreviousBlockId() != 0) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    // relying on cascade triggers in the database to delete the transactions for all deleted blocks
    @Override
    public void deleteBlocksFrom(long blockId) {
        if (!Db.isInTransaction()) {
            try {
                Db.beginTransaction();
                deleteBlocksFrom(blockId);
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
                throw e;
            } finally {
                Db.endTransaction();
            }
            return;
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT db_id FROM block WHERE db_id >= "
                     + "(SELECT db_id FROM block WHERE id = ?) ORDER BY db_id DESC");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?")) {
            try {
                pstmtSelect.setLong(1, blockId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    while (rs.next()) {
                        pstmtDelete.setInt(1, rs.getInt("db_id"));
                        pstmtDelete.executeUpdate();
                    }
                    Db.commitTransaction();
                }
            } catch (SQLException e) {
                Db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public abstract void deleteAll();

}
