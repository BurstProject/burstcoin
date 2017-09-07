package nxt.db;

/**
 * Created by jens on 10.08.2017.
 */
public interface VersionedEntityTable<T> extends DerivedTable, EntityTable<T>
{
    @Override
    void rollback(int height);

    boolean delete(T t);

    @Override
    void trim(int height);
}
