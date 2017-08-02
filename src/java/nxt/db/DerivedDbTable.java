package nxt.db;

import nxt.Nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DerivedDbTable {

    protected final String table;

    protected DerivedDbTable(String table) {
        this.table = table;
        Nxt.getBlockchainProcessor().registerDerivedTable(this);
    }

    public void rollback(int height) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = Db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + " WHERE height > ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void truncate() {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("DELETE FROM " + table);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void trim(int height) {
        //nothing to trim
    }

    public void finish() {

    }

}
