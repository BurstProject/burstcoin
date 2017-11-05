package brs.db.sql;

import brs.db.VersionedValuesTable;

public abstract class VersionedValuesSqlTable<T, V> extends ValuesSqlTable<T, V> implements VersionedValuesTable<T, V> {
    protected VersionedValuesSqlTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true);
    }

    @Override
    public final void rollback(int height) {
        VersionedEntitySqlTable.rollback(table, height, dbKeyFactory);
    }

    @Override
    public final void trim(int height) {
        VersionedEntitySqlTable.trim(table, height, dbKeyFactory);
    }
}
