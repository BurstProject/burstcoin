package brs.db;

import brs.db.sql.DbClause;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Created by jens on 10.08.2017.
 */
public interface EntityTable<T> extends DerivedTable
{
    void checkAvailable(int height);

    T get(BurstKey dbKey);

    T get(BurstKey dbKey, int height);

    T getBy(DbClause dbClause);

    T getBy(DbClause dbClause, int height);

    BurstIterator<T> getManyBy(DbClause dbClause, int from, int to);

    BurstIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort);

    BurstIterator<T> getManyBy(DbClause dbClause, int height, int from, int to);

    BurstIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort);

    BurstIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache);

    BurstIterator<T> getAll(int from, int to);

    BurstIterator<T> getAll(int from, int to, String sort);

    BurstIterator<T> getAll(int height, int from, int to);

    BurstIterator<T> getAll(int height, int from, int to, String sort);

    int getCount();

    int getRowCount();

    void insert(T t);

    @Override
    void rollback(int height);

    @Override
    void truncate();
}
