package nxt.db;

import nxt.db.sql.DbClause;
import nxt.db.sql.DbIterator;
import nxt.db.sql.DbKey;
import nxt.db.sql.NxtKey;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Created by jens on 10.08.2017.
 */
public interface VersionedBatchEntityTable<T> extends DerivedTable, EntityTable<T>
{
    boolean delete(T t);

    @Override
    T get(NxtKey dbKey);

    @Override
    void insert(T t);

    @Override
    void finish();

    @Override
    T get(NxtKey dbKey, int height);

    @Override
    T getBy(DbClause dbClause);

    @Override
    T getBy(DbClause dbClause, int height);

    @Override
    DbIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort);

    @Override
    DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to);

    @Override
    DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort);

    @Override
    DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache);

    @Override
    DbIterator<T> getAll(int from, int to);

    @Override
    DbIterator<T> getAll(int from, int to, String sort);

    @Override
    DbIterator<T> getAll(int height, int from, int to);

    @Override
    DbIterator<T> getAll(int height, int from, int to, String sort);

    @Override
    int getCount();

    @Override
    int getRowCount();

    @Override
    void rollback(int height);

    @Override
    void truncate();
}
