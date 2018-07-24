package brs.db.sql;

import brs.db.DerivedTable;
import brs.db.store.DerivedTableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jooq.impl.TableImpl;
import org.jooq.DSLContext;

public abstract class DerivedSqlTable implements DerivedTable {
  private static final Logger logger = LoggerFactory.getLogger(DerivedSqlTable.class);
  protected final String table;
  protected final TableImpl<?> tableClass;

  protected DerivedSqlTable(String table, TableImpl<?> tableClass, DerivedTableManager derivedTableManager) {
    this.table      = table;
    this.tableClass = tableClass;
    logger.trace("Creating derived table for "+table);
    derivedTableManager.registerDerivedTable(this);
  }

  @Override
  public void rollback(int height) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    DSLContext ctx = Db.getDSLContext();
    ctx.delete(tableClass).where(tableClass.field("height", Integer.class).gt(height)).execute();
  }

  @Override
  public void truncate() {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    DSLContext ctx = Db.getDSLContext();
    ctx.delete(tableClass).execute();
  }

  @Override
  public void trim(int height) {
    //nothing to trim
  }

  @Override
  public void finish() {

  }

}
