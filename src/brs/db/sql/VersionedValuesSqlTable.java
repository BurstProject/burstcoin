package brs.db.sql;

import brs.db.VersionedValuesTable;
import brs.db.store.DerivedTableManager;
import org.jooq.impl.TableImpl;

public abstract class VersionedValuesSqlTable<T, V> extends ValuesSqlTable<T, V> implements VersionedValuesTable<T, V> {
  protected VersionedValuesSqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory, DerivedTableManager derivedTableManager) {
    super(table, tableClass, dbKeyFactory, true, derivedTableManager);
  }

  @Override
  public final void rollback(int height) {
    VersionedEntitySqlTable.rollback(table, tableClass, height, dbKeyFactory);
  }

  @Override
  public final void trim(int height) {
    VersionedEntitySqlTable.trim(table, tableClass, height, dbKeyFactory);
  }
}
