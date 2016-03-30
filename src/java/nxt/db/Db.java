package nxt.db;

import nxt.Constants;
import nxt.Nxt;
import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public final class Db {

    private static final JdbcConnectionPool cp;
    private static volatile int maxActiveConnections;

    private static final ThreadLocal<DbConnection> localConnection = new ThreadLocal<>();
    private static final ThreadLocal<Map<String,Map<DbKey,Object>>> transactionCaches = new ThreadLocal<>();
    private static final ThreadLocal<Map<String,Map<DbKey,Object>>> transactionBatches = new ThreadLocal<>();

    private static final class DbConnection extends FilteredConnection {

        private DbConnection(Connection con) {
            super(con);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            throw new UnsupportedOperationException("Use Db.beginTransaction() to start a new transaction");
        }

        @Override
        public void commit() throws SQLException {
            if (localConnection.get() == null) {
                super.commit();
            } else if (! this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                throw new UnsupportedOperationException("Use Db.commitTransaction() to commit the transaction");
            }
        }

        private void doCommit() throws SQLException {
            super.commit();
        }

        @Override
        public void rollback() throws SQLException {
            if (localConnection.get() == null) {
                super.rollback();
            } else if (! this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                throw new UnsupportedOperationException("Use Db.rollbackTransaction() to rollback the transaction");
            }
        }

        private void doRollback() throws SQLException {
            super.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (localConnection.get() == null) {
                super.close();
            } else if (! this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            }
        }

    }

    public static void init() {}

    static {
        long maxCacheSize = Nxt.getIntProperty("nxt.dbCacheKB");
        if (maxCacheSize == 0) {
        	maxCacheSize = Math.min(256, Math.max(16, (Runtime.getRuntime().maxMemory() / (1024 * 1024) - 128)/2)) * 1024;
        }
        String dbUrl = Constants.isTestnet ? Nxt.getStringProperty("nxt.testDbUrl") : Nxt.getStringProperty("nxt.dbUrl");
        if (! dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
	// Replace old DB-Url if needed:
	if ( dbUrl.startsWith("jdbc:h2:burst_db")) {
	    dbUrl = "jdbc:h2:./burst_db" + dbUrl.substring(16);
	    Logger.logMessage(dbUrl);
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
    }
    
    public static void analyzeTables() {
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("ANALYZE SAMPLE_SIZE 0");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void shutdown() {
        try {
            Connection con = cp.getConnection();
            Statement stmt = con.createStatement();
            stmt.execute("SHUTDOWN COMPACT");
            Logger.logShutdownMessage("Database shutdown completed");
        } catch (SQLException e) {
            Logger.logShutdownMessage(e.toString(), e);
        }
    }

    private static Connection getPooledConnection() throws SQLException {
        Connection con = cp.getConnection();
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            Logger.logDebugMessage("Database connection pool current size: " + activeConnections);
        }
        return con;
    }

    public static Connection getConnection() throws SQLException {
        Connection con = localConnection.get();
        if (con != null) {
            return con;
        }
        con = getPooledConnection();
        con.setAutoCommit(true);
        return new DbConnection(con);
    }

    static Map<DbKey,Object> getCache(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey,Object> cacheMap = transactionCaches.get().get(tableName);
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
            transactionCaches.get().put(tableName, cacheMap);
        }
        return cacheMap;
    }

    static Map<DbKey,Object> getBatch(String tableName) {
        if(!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey,Object> batchMap = transactionBatches.get().get(tableName);
        if(batchMap == null) {
            batchMap = new HashMap<>();
            transactionBatches.get().put(tableName, batchMap);
        }
        return batchMap;
    }

    public static boolean isInTransaction() {
        return localConnection.get() != null;
    }

    public static Connection beginTransaction() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = getPooledConnection();
            con.setAutoCommit(false);
            con = new DbConnection(con);
            localConnection.set((DbConnection)con);
            transactionCaches.set(new HashMap<String, Map<DbKey, Object>>());
            transactionBatches.set(new HashMap<String, Map<DbKey, Object>>());
            return con;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void commitTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void rollbackTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doRollback();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        transactionCaches.get().clear();
        transactionBatches.get().clear();
    }

    public static void endTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        localConnection.set(null);
        transactionCaches.get().clear();
        transactionCaches.set(null);
        transactionBatches.get().clear();
        transactionBatches.set(null);
        DbUtils.close(con);
    }

    private Db() {} // never

}
