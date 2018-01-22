package brs.db;

import brs.BlockImpl;
import brs.BurstException;
import java.sql.ResultSet;
import org.jooq.DSLContext;
import brs.schema.tables.records.BlockRecord;

public interface BlockDb {
  BlockImpl findBlock(long blockId);

  boolean hasBlock(long blockId);

  long findBlockIdAtHeight(int height);

  BlockImpl findBlockAtHeight(int height);

  BlockImpl findLastBlock();

  BlockImpl findLastBlock(int timestamp);

  BlockImpl loadBlock(DSLContext ctx, ResultSet rs) throws BurstException.ValidationException;

  BlockImpl loadBlock(BlockRecord r) throws BurstException.ValidationException;

  void saveBlock(DSLContext ctx, BlockImpl block);

  // relying on cascade triggers in the database to delete the transactions for all deleted blocks
  void deleteBlocksFrom(long blockId);

  void deleteAll(boolean force);
}
