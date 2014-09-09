package nxt;

import nxt.util.Logger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

final class BlockDb {

    static BlockImpl findBlock(Long blockId) {
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
            throw new RuntimeException("Block already in database, id = " + blockId + ", does not pass validation!");
        }
    }

    static boolean hasBlock(Long blockId) {
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

    static long findBlockIdAtHeight(int height) {
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

    static BlockImpl findBlockAtHeight(int height) {
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
            throw new RuntimeException("Block already in database at height " + height + ", does not pass validation!");
        }
    }

    static BlockImpl loadBlock(Connection con, ResultSet rs) throws NxtException.ValidationException {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            Long previousBlockId = rs.getLong("previous_block_id");
            if (rs.wasNull()) {
                previousBlockId = null;
            }
            long totalAmountNQT = rs.getLong("total_amount");
            long totalFeeNQT = rs.getLong("total_fee");
            int payloadLength = rs.getInt("payload_length");
            byte[] generatorPublicKey = rs.getBytes("generator_public_key");
            byte[] previousBlockHash = rs.getBytes("previous_block_hash");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long baseTarget = rs.getLong("base_target");
            Long nextBlockId = rs.getLong("next_block_id");
            if (rs.wasNull()) {
                nextBlockId = null;
            }
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");

            Long id = rs.getLong("id");
            long nonce = rs.getLong("nonce");
            List<TransactionImpl> transactions = TransactionDb.findBlockTransactions(con, id);

            BlockImpl block = new BlockImpl(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                    generatorPublicKey, generationSignature, blockSignature, previousBlockHash, transactions,
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, nonce);

            for (TransactionImpl transaction : transactions) {
                transaction.setBlock(block);
            }

            return block;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void saveBlock(Connection con, BlockImpl block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, "
                    + "total_amount, total_fee, payload_length, generator_public_key, previous_block_hash, cumulative_difficulty, "
                    + "base_target, next_block_id, height, generation_signature, block_signature, payload_hash, generator_id, nonce) "
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, block.getId());
                pstmt.setInt(++i, block.getVersion());
                pstmt.setInt(++i, block.getTimestamp());
                if (block.getPreviousBlockId() != null) {
                    pstmt.setLong(++i, block.getPreviousBlockId());
                } else {
                    pstmt.setNull(++i, Types.BIGINT);
                }
                pstmt.setLong(++i, block.getTotalAmountNQT());
                pstmt.setLong(++i, block.getTotalFeeNQT());
                pstmt.setInt(++i, block.getPayloadLength());
                pstmt.setBytes(++i, block.getGeneratorPublicKey());
                pstmt.setBytes(++i, block.getPreviousBlockHash());
                pstmt.setBytes(++i, block.getCumulativeDifficulty().toByteArray());
                pstmt.setLong(++i, block.getBaseTarget());
                if (block.getNextBlockId()!= null) {
                    pstmt.setLong(++i, block.getNextBlockId());
                } else {
                    pstmt.setNull(++i, Types.BIGINT);
                }
                pstmt.setInt(++i, block.getHeight());
                pstmt.setBytes(++i, block.getGenerationSignature());
                pstmt.setBytes(++i, block.getBlockSignature());
                pstmt.setBytes(++i, block.getPayloadHash());
                pstmt.setLong(++i, block.getGeneratorId());
                pstmt.setLong(++i,  block.getNonce());
                pstmt.executeUpdate();
                TransactionDb.saveTransactions(con, block.getTransactions());
            }
            if (block.getPreviousBlockId() != null) {
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
    static void deleteBlocksFrom(Long blockId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT db_id FROM block WHERE db_id >= "
                     + "(SELECT db_id FROM block WHERE id = ?) ORDER BY db_id DESC");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?")) {
            try {
                pstmtSelect.setLong(1, blockId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    con.commit();
                    while (rs.next()) {
                        pstmtDelete.setInt(1, rs.getInt("db_id"));
                        pstmtDelete.executeUpdate();
                        con.commit();
                    }
                }
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void deleteAll() {
        Logger.logMessage("Deleting blockchain...");
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
