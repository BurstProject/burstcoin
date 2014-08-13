package nxt;

import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {

    private static volatile JdbcConnectionPool cp;
    private static volatile int maxActiveConnections;

    static void init() {
        long maxCacheSize = Nxt.getIntProperty("nxt.dbCacheKB");
        if (maxCacheSize == 0) {
            maxCacheSize = Runtime.getRuntime().maxMemory() / (1024 * 2);
        }
        String dbUrl = Constants.isTestnet ? Nxt.getStringProperty("nxt.testDbUrl") : Nxt.getStringProperty("nxt.dbUrl");
        if (! dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        Logger.logDebugMessage("Database jdbc url set to: " + dbUrl);
        cp = JdbcConnectionPool.create(dbUrl, "sa", "sa");
        cp.setMaxConnections(Nxt.getIntProperty("nxt.maxDbConnections"));
        cp.setLoginTimeout(Nxt.getIntProperty("nxt.dbLoginTimeout"));
        int defaultLockTimeout = Nxt.getIntProperty("nxt.dbDefaultLockTimeout") * 1000;
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        DbVersion.init();
    }

    static void shutdown() {
        if (cp != null) {
            try (Connection con = cp.getConnection();
                 Statement stmt = con.createStatement()) {
                stmt.execute("SHUTDOWN COMPACT");
                Logger.logMessage("Database shutdown completed");
            } catch (SQLException e) {
                Logger.logDebugMessage(e.toString(), e);
            }
            //cp.dispose();
            cp = null;
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection con = cp.getConnection();
        con.setAutoCommit(false);
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            Logger.logDebugMessage("Database connection pool current size: " + activeConnections);
        }
        return con;
    }

    private Db() {} // never

}
