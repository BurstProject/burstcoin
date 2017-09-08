package nxt.db.firebird;

import nxt.BlockImpl;
import nxt.Nxt;
import nxt.NxtException;
import nxt.db.sql.Db;
import nxt.db.sql.DbUtils;
import nxt.db.sql.SqlBlockDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

class FirebirdBlockDB extends SqlBlockDb {
    private static final Logger logger = LoggerFactory.getLogger(FirebirdBlockDB.class);

    @Override
    public void deleteAll() {
        if (!Db.isInTransaction()) {
            try {
                Db.beginTransaction();
                deleteAll();
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
                throw e;
            } finally {
                Db.endTransaction();
            }
            return;
        }
        logger.info("Deleting blockchain...");
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                Db.commitTransaction();
            } catch (SQLException e) {
                Db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void saveBlock(Connection con, BlockImpl block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, \"timestamp\", previous_block_id, "
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

    @Override
    public BlockImpl findLastBlock(int timestamp) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE \"timestamp\" <= ? ORDER BY \"timestamp\" DESC" + DbUtils.limitsClause(1) )) {
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

}
