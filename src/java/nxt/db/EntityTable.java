package nxt.db;

import nxt.db.sql.DbClause;
import nxt.db.sql.DbIterator;
import nxt.db.sql.DbKey;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Created by jens on 10.08.2017.
 */
public interface EntityTable<T> extends DerivedTable
{
    void checkAvailable(int height);

    T get(DbKey dbKey);

    T get(DbKey dbKey, int height);

    T getBy(DbClause dbClause);

    T getBy(DbClause dbClause, int height);

    DbIterator<T> getManyBy(DbClause dbClause, int from, int to);

    DbIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort);

    DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to);

    DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort);

    DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache);

    DbIterator<T> getAll(int from, int to);

    DbIterator<T> getAll(int from, int to, String sort);

    DbIterator<T> getAll(int height, int from, int to);

    DbIterator<T> getAll(int height, int from, int to, String sort);

    int getCount();

    int getRowCount();

    void insert(T t);

    @Override
    void rollback(int height);

    @Override
    void truncate();
}
