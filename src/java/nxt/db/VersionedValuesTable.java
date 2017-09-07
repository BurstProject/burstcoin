package nxt.db;

/**
 * Created by jens on 10.08.2017.
 */
public interface VersionedValuesTable<T, V> extends DerivedTable, ValuesTable<T, V>
{
    @Override
    void rollback(int height);

    @Override
    void trim(int height);
}
