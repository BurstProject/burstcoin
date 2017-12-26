package nxt.db;

import nxt.Nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class VersionedEntityDbTable<T> extends EntityDbTable<T> {

    protected VersionedEntityDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true);
    }

    @Override
    public void rollback(int height) {
        rollback(table, height, dbKeyFactory);
    }

    public boolean delete(T t) {
        if (t == null) {
            return false;
        }
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtCount = con.prepareStatement("SELECT COUNT(*) AS count FROM " + table + dbKeyFactory.getPKClause()
                + " AND height < ?")) {
            int i = dbKey.setPK(pstmtCount);
            pstmtCount.setInt(i, Nxt.getBlockchain().getHeight());
            try (ResultSet rs = pstmtCount.executeQuery()) {
                rs.next();
                if (rs.getInt("count") > 0) {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
                        dbKey.setPK(pstmt);
                        pstmt.executeUpdate();
                        save(con, t);
                        pstmt.executeUpdate(); // delete after the save
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause())) {
                        dbKey.setPK(pstmtDelete);
                        return pstmtDelete.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            Db.getCache(table).remove(dbKey);
        }
    }

    @Override
    public final void trim(int height) {
        trim(table, height, dbKeyFactory);
    }

    static void rollback(final String table, final int height, final DbKey.Factory dbKeyFactory) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + dbKeyFactory.getPKColumns()
                     + " FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                     + " WHERE height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table
                     + " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
                     + " (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + ")")) {
            pstmtSelectToDelete.setInt(1, height);
            List<DbKey> dbKeys = new ArrayList<>();
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    dbKeys.add(dbKeyFactory.newKey(rs));
                }
            }
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
            for (DbKey dbKey : dbKeys) {
                int i = 1;
                i = dbKey.setPK(pstmtSetLatest, i);
                i = dbKey.setPK(pstmtSetLatest, i);
                pstmtSetLatest.executeUpdate();
                //Db.getCache(table).remove(dbKey);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        Db.getCache(table).clear();
    }

    static void trim(final String table, final int height, final DbKey.Factory dbKeyFactory) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + dbKeyFactory.getPKColumns() + ", MAX(height) AS max_height"
                     + " FROM " + table + " WHERE height < ? GROUP BY " + dbKeyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height < ?");
            PreparedStatement pstmtDeleteDeleted = con.prepareStatement("DELETE FROM " + table + " WHERE height < ? AND latest = FALSE "
                    + " AND (" + dbKeyFactory.getPKColumns() + ") NOT IN (SELECT (" + dbKeyFactory.getPKColumns() + ") FROM "
                    + table + " WHERE height >= ?)")) {
            pstmtSelect.setInt(1, height);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                while (rs.next()) {
                    DbKey dbKey = dbKeyFactory.newKey(rs);
                    int maxHeight = rs.getInt("max_height");
                    int i = 1;
                    i = dbKey.setPK(pstmtDelete, i);
                    pstmtDelete.setInt(i, maxHeight);
                    pstmtDelete.executeUpdate();
                }
                pstmtDeleteDeleted.setInt(1, height);
                pstmtDeleteDeleted.setInt(2, height);
                pstmtDeleteDeleted.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
