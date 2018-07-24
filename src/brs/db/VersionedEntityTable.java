package brs.db;

public interface VersionedEntityTable<T> extends DerivedTable, EntityTable<T> {
    @Override
    void rollback(int height);

    boolean delete(T t);

    @Override
    void trim(int height);
}
