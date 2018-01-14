package brs.db.sql;

import brs.Burst;
import brs.db.DerivedTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import org.jooq.impl.TableImpl;
import org.jooq.DSLContext;

public abstract class DerivedSqlTable implements DerivedTable {
  private static final Logger logger = LoggerFactory.getLogger(DerivedSqlTable.class);
  protected final String table;
  protected final TableImpl<?> tableClass;

  protected DerivedSqlTable(String table, TableImpl<?> tableClass) {
    this.table      = table;
    this.tableClass = tableClass;
    logger.trace("Creating derived table for "+table);
    Burst.getBlockchainProcessor().registerDerivedTable(this);
  }

  @Override
  public void rollback(int height) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    try ( DSLContext ctx = Db.getDSLContext() ) {
      ctx.delete(tableClass).where(tableClass.field("height", Integer.class).gt(height)).execute();
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public void truncate() {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    try (DSLContext ctx = Db.getDSLContext() ) {
      ctx.delete(tableClass).execute();
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public void trim(int height) {
    //nothing to trim
  }

  @Override
  public void finish() {

  }

}
