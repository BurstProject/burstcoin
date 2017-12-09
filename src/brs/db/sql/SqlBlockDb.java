package brs.db.sql;

import brs.BlockImpl;
import brs.Burst;
import brs.BurstException;
import brs.db.BlockDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static brs.schema.Tables.*;
import static org.jooq.impl.DSL.*;

import org.jooq.DSLContext;
import org.jooq.Record;

public abstract class SqlBlockDb implements BlockDb {

  private static final Logger logger = LoggerFactory.getLogger(BlockDb.class);

  public BlockImpl findBlock(long blockId) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return ctx.selectFrom(BLOCK).where(BLOCK.ID.eq(blockId)).fetchAny().into(BlockImpl.class);
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  public boolean hasBlock(long blockId) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return ctx.fetchExists(ctx.selectOne().from(BLOCK).where(BLOCK.ID.eq(blockId)));
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  public long findBlockIdAtHeight(int height) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      Long id = ctx.select(BLOCK.ID).from(BLOCK).where(BLOCK.HEIGHT.eq(height)).fetchOne(BLOCK.ID);
      if ( id == null ) {
        throw new RuntimeException("Block at height " + height + " not found in database!");
      }
      return id;
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  public BlockImpl findBlockAtHeight(int height) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      BlockImpl block = ctx.selectFrom(BLOCK).where(BLOCK.HEIGHT.eq(height)).fetchAny().into(BlockImpl.class);
      if ( block == null ) {
          throw new RuntimeException("Block at height " + height + " not found in database!");
      }
      return block;
    } catch (Exception e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  public BlockImpl findLastBlock() {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return ctx.selectFrom(BLOCK).orderBy(BLOCK.DB_ID.desc()).limit(1).fetchAny().into(BlockImpl.class);
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    } catch (Exception e) {
      throw new RuntimeException("Last block already in database does not pass validation!", e);
    }
  }

  public BlockImpl findLastBlock(int timestamp) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return ctx.selectFrom(BLOCK).where(BLOCK.TIMESTAMP.lessOrEqual(timestamp)).orderBy(BLOCK.DB_ID.desc()).limit(1).fetchAny().into(BlockImpl.class);
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    } catch (Exception e) {
      throw new RuntimeException("Block already in database at timestamp " + timestamp + " does not pass validation!", e);
    }
  }

  public BlockImpl loadBlock(Connection con, ResultSet rs) throws BurstException.ValidationException {
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
  
  public void saveBlock(Connection con, BlockImpl block) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      ctx.insertInto(
        BLOCK,
        BLOCK.ID, BLOCK.VERSION, BLOCK.TIMESTAMP, BLOCK.PREVIOUS_BLOCK_ID, BLOCK.TOTAL_AMOUNT, BLOCK.TOTAL_FEE,
        BLOCK.PAYLOAD_LENGTH, BLOCK.GENERATOR_PUBLIC_KEY, BLOCK.PREVIOUS_BLOCK_HASH, BLOCK.CUMULATIVE_DIFFICULTY,
        BLOCK.BASE_TARGET, BLOCK.HEIGHT, BLOCK.GENERATION_SIGNATURE, BLOCK.BLOCK_SIGNATURE, BLOCK.PAYLOAD_HASH,
        BLOCK.GENERATOR_ID, BLOCK.NONCE, BLOCK.ATS
      ).values(
        block.getId(), block.getVersion(), block.getTimestamp(), block.getPreviousBlockId(), block.getTotalAmountNQT(), block.getTotalFeeNQT(),
        block.getPayloadLength(), block.getGeneratorPublicKey(), block.getPreviousBlockHash(), block.getCumulativeDifficulty().toByteArray(),
        block.getBaseTarget(), block.getHeight(), block.getGenerationSignature(), block.getBlockSignature(), block.getPayloadHash(),
        block.getGeneratorId(), block.getNonce(), block.getBlockATs()
      ).execute();

      Burst.getDbs().getTransactionDb().saveTransactions(block.getTransactions());

      if (block.getPreviousBlockId() != 0) {
        ctx.update(BLOCK).set(BLOCK.NEXT_BLOCK_ID, block.getId()).where(BLOCK.ID.eq(block.getPreviousBlockId()));
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
