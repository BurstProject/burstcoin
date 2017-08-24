package nxt;

import nxt.db.sql.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class DbVersion {

    private static final Logger logger = LoggerFactory.getLogger(DbVersion.class);

    static void init() {
        try (Connection con = Db.beginTransaction(); Statement stmt = con.createStatement()) {
            int nextUpdate = 1;
            try {
                ResultSet rs = stmt.executeQuery("SELECT next_update FROM version");
                if (! rs.next()) {
                    throw new RuntimeException("Invalid version table");
                }
                nextUpdate = rs.getInt("next_update");
                if (! rs.isLast()) {
                    throw new RuntimeException("Invalid version table");
                }
                rs.close();
                logger.info("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
            } catch (SQLException e) {
                logger.info("Initializing an empty database");
                stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
                stmt.executeUpdate("INSERT INTO version VALUES (1)");
                Db.commitTransaction();
            }
            update(nextUpdate);
        } catch (SQLException e) {
            Db.rollbackTransaction();
            throw new RuntimeException(e.toString(), e);
        } finally {
            Db.endTransaction();
        }

    }

    private static void apply(String sql) {
        try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
            try {
                if (sql != null) {
                    logger.debug("Will apply sql:\n" + sql);
                    stmt.executeUpdate(sql);
                }
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                Db.commitTransaction();
            } catch (Exception e) {
                Db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error executing " + sql, e);
        }
    }

    private static void update(int nextUpdate) {
        if ( nextUpdate < 163 ) {
            apply(null);
        }
        else {
            switch (nextUpdate) {
                case 163:
                	apply("ALTER TABLE alias ALTER COLUMN alias_name_LOWER SET DEFAULT '';");
                case 164:
                	apply("ALTER DATABASE burstwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                case 165:
                	apply("ALTER TABLE alias CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                case 166:
                	apply("ALTER TABLE account CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                case 167:
                	apply("ALTER TABLE asset CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                case 168:
                	apply("ALTER TABLE goods CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                case 169:
                	apply("ALTER TABLE at CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
                case 170:
                	return;
                default:
                    throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
            }
        }
    }

    private DbVersion() {} //never
}
