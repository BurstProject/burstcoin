package nxt.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

public abstract class VersionedBatchEntityDbTable<T> extends VersionedEntityDbTable<T> {
    protected VersionedBatchEntityDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    protected abstract String updateQuery();
    protected abstract void batch(PreparedStatement pstmt, T t) throws SQLException;

    @Override
    public boolean delete(T t) {
        if(!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        Db.getBatch(table).put(dbKey, null);
        Db.getCache(table).remove(dbKey);

        return true;
    }

    @Override
    public T get(DbKey dbKey) {
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
        DbKey dbKey = dbKeyFactory.newKey(t);
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
                + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
            Iterator<DbKey> it = Db.getBatch(table).keySet().iterator();
            while(it.hasNext()) {
                DbKey key = it.next();
                key.setPK(pstmt);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
        catch(SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }

        try(Connection con =Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(updateQuery())) {
            Iterator<Map.Entry<DbKey,Object>> it = Db.getBatch(table).entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<DbKey,Object> entry = it.next();
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
    public T get(DbKey dbKey, int height) {
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
    public DbIterator<T> getManyBy(DbClause dbClause, int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, from, to);
    }

    @Override
    public DbIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, from, to, sort);
    }

    @Override
    public DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, height, from, to);
    }

    @Override
    public DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(dbClause, height, from, to, sort);
    }

    @Override
    public DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(con, pstmt, cache);
    }

    @Override
    public DbIterator<T> getAll(int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(from, to);
    }

    @Override
    public DbIterator<T> getAll(int from, int to, String sort) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(from, to, sort);
    }

    @Override
    public DbIterator<T> getAll(int height, int from, int to) {
        if(Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(height, from, to);
    }

    @Override
    public DbIterator<T> getAll(int height, int from, int to, String sort) {
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
