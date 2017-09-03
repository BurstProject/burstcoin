package nxt.db.mariadb;

import nxt.db.sql.Db;
import nxt.db.sql.SqlBlockDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

class MariadbBlockDB extends SqlBlockDb {
    private static final Logger logger = LoggerFactory.getLogger(MariadbBlockDB.class);

    @Override
    public void deleteAll() {
        if (!Db.isInTransaction()) {
            try {
                Db.beginTransaction();
                deleteAll();
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
                throw e;
            } finally {
                Db.endTransaction();
            }
            return;
        }
        logger.info("Deleting blockchain...");
        try (Connection con = Db.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET foreign_key_checks = 0");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                stmt.executeUpdate("SET foreign_key_checks = 1");
                Db.commitTransaction();
            } catch (SQLException e) {
                Db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
