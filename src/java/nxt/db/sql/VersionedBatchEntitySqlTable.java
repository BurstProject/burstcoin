package nxt.db.sql;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import nxt.Nxt;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedBatchEntityTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public abstract class VersionedBatchEntitySqlTable<T> extends VersionedEntitySqlTable<T> implements VersionedBatchEntityTable<T>
{
    protected VersionedBatchEntitySqlTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    protected abstract String updateQuery();
    protected abstract void batch(PreparedStatement pstmt, T t) throws SQLException;


    @Override
    public boolean delete(T t) {
        if(!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = (DbKey)dbKeyFactory.newKey(t);
        Db.getBatch(table).put(dbKey, null);
        Db.getCache(table).remove(dbKey);

        return true;
    }

    @Override
    public T get(NxtKey dbKey) {
        if(Db.isInTransaction()) {
            if(Db.getBatch(table).containsKey(dbKey)) {
                return (T)Db.getBatch(table).get(dbKey);
            }
        }
        return super.get(dbKey);
    }

    @Override
    public void insert(T t) {
        if(!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = (DbKey)dbKeyFactory.newKey(t);
        Db.getBatch(table).put(dbKey, t);
        Db.getCache(table).put(dbKey, t);
    }

    @Override
    public void finish() {
        if(!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }

        try(Connection con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE" + DbUtils.limitsClause(1))) {
            Set keySet = Db.getBatch(table).keySet();
            Iterator<DbKey> it = keySet.iterator();
            while(it.hasNext()) {
                DbKey key = it.next();
                key.setPK(pstmt);
                DbUtils.setLimits(2, pstmt, 1);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
        catch(SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }

        try(Connection con =Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(updateQuery())) {

            List<Map.Entry<DbKey,Object>> entries = new ArrayList<>(Db.getBatch(table).entrySet());
            for ( Map.Entry<DbKey,Object> entry: entries)
            {
                if(entry.getValue() != null) {
                    batch(pstmt, (T)entry.getValue());
                }
            }
            pstmt.executeBatch();

        }
        catch(SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public T get(NxtKey dbKey, int height) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.get(dbKey, height);
    }

    @Override
    public T getBy(DbClause dbClause) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getBy(dbClause);
    }

    @Override
    public T getBy(DbClause dbClause, int height) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getBy(dbClause, height);
    }

    @Override
    public NxtIterator<T> getManyBy(DbClause dbClause, int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, from, to);
    }

    @Override
    public NxtIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, from, to, sort);
    }

    @Override
    public NxtIterator<T> getManyBy(DbClause dbClause, int height, int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, height, from, to);
    }

    @Override
    public NxtIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, height, from, to, sort);
    }

    @Override
    public NxtIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(con, pstmt, cache);
    }

    @Override
    public NxtIterator<T> getAll(int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(from, to);
    }

    @Override
    public NxtIterator<T> getAll(int from, int to, String sort) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(from, to, sort);
    }

    @Override
    public NxtIterator<T> getAll(int height, int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(height, from, to);
    }

    @Override
    public NxtIterator<T> getAll(int height, int from, int to, String sort) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(height, from, to, sort);
    }

    @Override
    public int getCount() {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getCount();
    }

    @Override
    public int getRowCount() {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getRowCount();
    }

    @Override
    public void rollback(int height) {
        super.rollback(height);
        Db.getBatch(table).clear();
    }

    @Override
    public void truncate() {
        super.truncate();
        Db.getBatch(table).clear();
    }
}
