package brs.db;

import brs.db.sql.DbKey;

import java.util.List;

/**
 * Created by jens on 10.08.2017.
 */
public interface ValuesTable<T, V> extends DerivedTable
{
    List<V> get(NxtKey dbKey);

    void insert(T t, List<V> values);

    @Override
    void rollback(int height);

    @Override
    void truncate();
}
