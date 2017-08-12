package nxt.db.sql;

import nxt.Nxt;
import nxt.db.DerivedTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DerivedSqlTable implements DerivedTable
{

    protected final String table;

    protected DerivedSqlTable(String table) {
        this.table = table;
        Nxt.getBlockchainProcessor().registerDerivedTable(this);
    }

    @Override
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

    @Override
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

    @Override
    public void trim(int height) {
        //nothing to trim
    }

    @Override
    public void finish() {

    }

}
