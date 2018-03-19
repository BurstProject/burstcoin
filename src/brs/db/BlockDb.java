package brs.db;

import brs.Block;
import brs.BurstException;
import java.sql.ResultSet;
import org.jooq.DSLContext;
import brs.schema.tables.records.BlockRecord;

public interface BlockDb {
  Block findBlock(long blockId);

  boolean hasBlock(long blockId);

  long findBlockIdAtHeight(int height);

  Block findBlockAtHeight(int height);

  Block findLastBlock();

  Block findLastBlock(int timestamp);

  Block loadBlock(DSLContext ctx, ResultSet rs) throws BurstException.ValidationException;

  Block loadBlock(BlockRecord r) throws BurstException.ValidationException;

  void saveBlock(DSLContext ctx, Block block);

  // relying on cascade triggers in the database to delete the transactions for all deleted blocks
  void deleteBlocksFrom(long blockId);

  void deleteAll(boolean force);
}
