package brs.db.sql;

import brs.db.VersionedValuesTable;
import org.jooq.impl.TableImpl;

public abstract class VersionedValuesSqlTable<T, V> extends ValuesSqlTable<T, V> implements VersionedValuesTable<T, V> {
  protected VersionedValuesSqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory) {
    super(table, tableClass, dbKeyFactory, true);
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
