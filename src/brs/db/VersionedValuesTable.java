package brs.db;

public interface VersionedValuesTable<T, V> extends DerivedTable, ValuesTable<T, V> {
    @Override
    void rollback(int height);

    @Override
    void trim(int height);
}
