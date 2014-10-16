package nxt.db;

public abstract class VersionedValuesDbTable<T, V> extends ValuesDbTable<T, V> {

    protected VersionedValuesDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true);
    }

    @Override
    public final void rollback(int height) {
        VersionedEntityDbTable.rollback(table, height, dbKeyFactory);
    }

    @Override
    public final void trim(int height) {
        VersionedEntityDbTable.trim(table, height, dbKeyFactory);
    }

}
