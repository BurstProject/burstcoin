package nxt.db;

import nxt.db.sql.DbClause;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Created by jens on 10.08.2017.
 */
public interface EntityTable<T> extends DerivedTable
{
    void checkAvailable(int height);

    T get(NxtKey dbKey);

    T get(NxtKey dbKey, int height);

    T getBy(DbClause dbClause);

    T getBy(DbClause dbClause, int height);

    NxtIterator<T> getManyBy(DbClause dbClause, int from, int to);

    NxtIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort);

    NxtIterator<T> getManyBy(DbClause dbClause, int height, int from, int to);

    NxtIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort);

    NxtIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache);

    NxtIterator<T> getAll(int from, int to);

    NxtIterator<T> getAll(int from, int to, String sort);

    NxtIterator<T> getAll(int height, int from, int to);

    NxtIterator<T> getAll(int height, int from, int to, String sort);

    int getCount();

    int getRowCount();

    void insert(T t);

    @Override
    void rollback(int height);

    @Override
    void truncate();
}
