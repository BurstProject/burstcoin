package nxt.db.h2;

import nxt.db.sql.Db;
import nxt.db.sql.SqlBlockDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

class H2BlockDB extends SqlBlockDb {
    private static final Logger logger = LoggerFactory.getLogger(H2BlockDB.class);

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
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
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
