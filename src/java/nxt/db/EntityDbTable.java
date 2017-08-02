package nxt.db;

import nxt.Nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class EntityDbTable<T> extends DerivedDbTable {

    private final boolean multiversion;
    protected final DbKey.Factory<T> dbKeyFactory;
    private final String defaultSort;

    protected EntityDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        this(table, dbKeyFactory, false);
    }

    EntityDbTable(String table, DbKey.Factory<T> dbKeyFactory, boolean multiversion) {
        super(table);
        this.dbKeyFactory = dbKeyFactory;
        this.multiversion = multiversion;
        this.defaultSort = " ORDER BY " + (multiversion ? dbKeyFactory.getPKColumns() : " height DESC ");
    }

    protected abstract T load(Connection con, ResultSet rs) throws SQLException;

    protected void save(Connection con, T t) throws SQLException {
    }

    protected String defaultSort() {
        return defaultSort;
    }

    public final void checkAvailable(int height) {
        if (multiversion && height < Nxt.getBlockchainProcessor().getMinRollbackHeight()) {
            throw new IllegalArgumentException("Historical data as of height " + height +" not available, set nxt.trimDerivedTables=false and re-scan");
        }
    }

    public T get(DbKey dbKey) {
        if (Db.isInTransaction()) {
            T t = (T)Db.getCache(table).get(dbKey);
            if (t != null) {
                return t;
            }
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause()
             + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            dbKey.setPK(pstmt);
            return get(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public T get(DbKey dbKey, int height) {
        checkAvailable(height);
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height <= ?" + (multiversion ? " AND (latest = TRUE OR EXISTS ("
                     + "SELECT 1 FROM " + table + dbKeyFactory.getPKClause() + " AND height > ?)) ORDER BY height DESC LIMIT 1" : ""))) {
            int i = dbKey.setPK(pstmt);
            pstmt.setInt(i, height);
            if (multiversion) {
                i = dbKey.setPK(pstmt, ++i);
                pstmt.setInt(i, height);
            }
            return get(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public T getBy(DbClause dbClause) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table
                     + " WHERE " + dbClause.getClause() + (multiversion ? " AND latest = TRUE LIMIT 1" : ""))) {
            dbClause.set(pstmt, 1);
            return get(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public T getBy(DbClause dbClause, int height) {
        checkAvailable(height);
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + " AS a WHERE " + dbClause.getClause()
                     + " AND height <= ?" + (multiversion ? " AND (latest = TRUE OR EXISTS ("
                     + "SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause()
                     + " AND b.height > ?)) ORDER BY height DESC LIMIT 1" : ""))) {
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            pstmt.setInt(i, height);
            if (multiversion) {
                pstmt.setInt(++i, height);
            }
            return get(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private T get(Connection con, PreparedStatement pstmt, boolean cache) throws SQLException {
        final boolean doCache = cache && Db.isInTransaction();
        try (ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            T t = null;
            DbKey dbKey = null;
            if (doCache) {
                dbKey = dbKeyFactory.newKey(rs);
                t = (T) Db.getCache(table).get(dbKey);
            }
            if (t == null) {
                t = load(con, rs);
                if (doCache) {
                    Db.getCache(table).put(dbKey, t);
                }
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return t;
        }
    }

    public DbIterator<T> getManyBy(DbClause dbClause, int from, int to) {
        return getManyBy(dbClause, from, to, defaultSort());
    }

    public DbIterator<T> getManyBy(DbClause dbClause, int from, int to, String sort) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table
                    + " WHERE " + dbClause.getClause() + (multiversion ? " AND latest = TRUE " : " ") + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            i = DbUtils.setLimits(i, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to) {
        return getManyBy(dbClause, height, from, to, defaultSort());
    }

    public DbIterator<T> getManyBy(DbClause dbClause, int height, int from, int to, String sort) {
        checkAvailable(height);
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + " AS a WHERE " + dbClause.getClause()
                    + "AND a.height <= ?" + (multiversion ? " AND (a.latest = TRUE OR (a.latest = FALSE "
                    + "AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause() + " AND b.height > ?) "
                    + "AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE " + dbKeyFactory.getSelfJoinClause()
                    + " AND b.height <= ? AND b.height > a.height))) "
                    : " ") + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            pstmt.setInt(i, height);
            if (multiversion) {
                pstmt.setInt(++i, height);
                pstmt.setInt(++i, height);
            }
            i = DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<T> getManyBy(Connection con, PreparedStatement pstmt, boolean cache) {
        final boolean doCache = cache && Db.isInTransaction();
        return new DbIterator<>(con, pstmt, new DbIterator.ResultSetReader<T>() {
            @Override
            public T get(Connection con, ResultSet rs) throws Exception {
                T t = null;
                DbKey dbKey = null;
                if (doCache) {
                    dbKey = dbKeyFactory.newKey(rs);
                    t = (T) Db.getCache(table).get(dbKey);
                }
                if (t == null) {
                    t = load(con, rs);
                    if (doCache) {
                        Db.getCache(table).put(dbKey, t);
                    }
                }
                return t;
            }
        });
    }

    public DbIterator<T> getAll(int from, int to) {
        return getAll(from, to, defaultSort());
    }

    public DbIterator<T> getAll(int from, int to, String sort) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table
                     + (multiversion ? " WHERE latest = TRUE " : " ") + sort
                    + DbUtils.limitsClause(from, to));
            DbUtils.setLimits(1, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<T> getAll(int height, int from, int to) {
        return getAll(height, from, to, defaultSort());
    }

    public DbIterator<T> getAll(int height, int from, int to, String sort) {
        checkAvailable(height);
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + " AS a WHERE height <= ?"
                    + (multiversion ? " AND (latest = TRUE OR (latest = FALSE "
                    + "AND EXISTS (SELECT 1 FROM " + table + " AS b WHERE b.height > ? AND " + dbKeyFactory.getSelfJoinClause()
                    + ") AND NOT EXISTS (SELECT 1 FROM " + table + " AS b WHERE b.height <= ? AND " + dbKeyFactory.getSelfJoinClause()
                    + " AND b.height > a.height))) " : " ") + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setInt(++i, height);
            if (multiversion) {
                pstmt.setInt(++i, height);
                pstmt.setInt(++i, height);
            }
            i = DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int getCount() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table
                     + (multiversion ? " WHERE latest = TRUE" : ""));
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int getRowCount() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void insert(T t) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        T cachedT = (T)Db.getCache(table).get(dbKey);
        if (cachedT == null) {
            Db.getCache(table).put(dbKey, t);
        } else if (t != cachedT) { // not a bug
            throw new IllegalStateException("Different instance found in Db cache, perhaps trying to save an object "
                    + "that was read outside the current transaction");
        }
        try (Connection con = Db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                        + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
                    dbKey.setPK(pstmt);
                    pstmt.executeUpdate();
                }
            }
            save(con, t);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void rollback(int height) {
        super.rollback(height);
        Db.getCache(table).clear();
    }

    @Override
    public void truncate() {
        super.truncate();
        Db.getCache(table).clear();
    }

}
