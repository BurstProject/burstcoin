package nxt.db.sql;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.gquintana.metrics.sql.MetricsSql;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import nxt.Constants;
import nxt.Nxt;
import org.firebirdsql.gds.impl.GDSType;
import org.firebirdsql.management.FBManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class Db {

    private static final Logger logger = LoggerFactory.getLogger(Db.class);

    private static final ComboPooledDataSource cpds;
    private static final ThreadLocal<DbConnection> localConnection = new ThreadLocal<>();
    private static final ThreadLocal<Timer.Context> localTimerContext = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Map<DbKey, Object>>> transactionCaches = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Map<DbKey, Object>>> transactionBatches = new ThreadLocal<>();
    private static final TYPE DATABASE_TYPE;
    private static final Timer transactionTimer = Nxt.metrics.timer(MetricRegistry.name(Db.class, "transaction"));
    private static final Counter commitCounter = Nxt.metrics.counter(MetricRegistry.name(Db.class, "commits"));
    private static final Counter rollbackCounter = Nxt.metrics.counter(MetricRegistry.name(Db.class, "rollbacks"));
    private static volatile int maxActiveConnections;

    static {
        String dbUrl;
        String dbUsername;
        String dbPassword;

        if (Constants.isTestnet) {
            dbUrl = Nxt.getStringProperty("nxt.testDbUrl");
            dbUsername = Nxt.getStringProperty("nxt.testDbUsername");
            dbPassword = Nxt.getStringProperty("nxt.testDbPassword");
        } else {
            dbUrl = Nxt.getStringProperty("nxt.dbUrl");
            dbUsername = Nxt.getStringProperty("nxt.dbUsername");
            dbPassword = Nxt.getStringProperty("nxt.dbPassword");
        }

        logger.debug("Database jdbc url set to: " + dbUrl);
        try {
            DATABASE_TYPE = TYPE.getTypeFromJdbcUrl(dbUrl);

            cpds = new ComboPooledDataSource();

            cpds.setJdbcUrl(dbUrl);

            cpds.setMaxPoolSize(Nxt.getIntProperty("nxt.dbMaximumPoolSize"));
            cpds.setAutoCommitOnClose(false);
            cpds.setMaxStatements(1024);
            cpds.setMaxStatementsPerConnection(128);
            cpds.setMaxConnectionAge(120);

            Properties config = new Properties();
            switch (DATABASE_TYPE) {
                case MARIADB:

                    config.put("cachePrepStmts", "true");
                    config.put("prepStmtCacheSize", "250");
                    config.put("prepStmtCacheSqlLimit", "2048");
                    config.put("characterEncoding", "utf8mb4");
                    config.put("useUnicode", "true");
                    config.put("useServerPrepStmts", "false");
                    config.put("rewriteBatchedStatements", "true");
                    break;
                case FIREBIRD:

                    config.put("encoding", "UTF8");
                    config.put("cachePrepStmts", "true");
                    config.put("prepStmtCacheSize", "250");
                    config.put("prepStmtCacheSqlLimit", "2048");
                    config.put("encoding", "UTF8");

                    if (dbUrl.startsWith("jdbc:firebirdsql:embedded:")) {
                        String firebirdDb = dbUrl.replaceFirst("^jdbc:firebirdsql:embedded:", "").replaceFirst("\\?.*$", "");

                        if (!new File(firebirdDb).isFile()) {
                            FBManager manager = new FBManager(GDSType.getType("EMBEDDED"));
                            manager.start();
                            manager.createDatabase(firebirdDb, "", "");
                            manager.stop();
                        }
                    }

                    break;
                case H2:
                    config.put("cachePrepStmts", "true");
                    config.put("prepStmtCacheSize", "250");
                    config.put("prepStmtCacheSqlLimit", "2048");
                    break;
            }
            if (dbUsername != null)
                cpds.setUser(dbUsername);
            if (dbPassword != null)
                cpds.setPassword(dbPassword);

//            cp = new HikariDataSource(config);

            if (DATABASE_TYPE == TYPE.H2) {
                int defaultLockTimeout = Nxt.getIntProperty("nxt.dbDefaultLockTimeout") * 1000;
                try (Connection con = cpds.getConnection();
                     Statement stmt = con.createStatement()) {
                    stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    private Db() {
    } // never

    public static void init() {
    }

    public static void analyzeTables() {
        if (DATABASE_TYPE == TYPE.H2) {
            try (Connection con = cpds.getConnection();
                 Statement stmt = con.createStatement()) {
                stmt.execute("ANALYZE SAMPLE_SIZE 0");
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

    public static void shutdown() {
        try {
            Connection con = cpds.getConnection();
            if (DATABASE_TYPE == TYPE.H2) {
                logger.info("Compacting database - this may take a while");
                Statement stmt = con.createStatement();
                stmt.execute("SHUTDOWN COMPACT");
            }
            logger.info("Database shutdown completed");
        } catch (SQLException e) {
            logger.info(e.toString(), e);
        }
    }

    private static Connection getPooledConnection() throws SQLException {
        Connection con = cpds.getConnection();
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

    public static Connection getRawConnection() throws SQLException {

        Connection con = getPooledConnection();
        return con;
    }

    static Map<DbKey, Object> getCache(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey, Object> cacheMap = transactionCaches.get().get(tableName);
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
            transactionCaches.get().put(tableName, cacheMap);
        }
        return cacheMap;
    }

    static Map<DbKey, Object> getBatch(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey, Object> batchMap = transactionBatches.get().get(tableName);
        if (batchMap == null) {
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
            // Now for the interesting part
//            if (DATABASE_TYPE == TYPE.FIREBIRD)
//            {
//                Field field = ReflectionUtils.getFields(ProxyConnection.class, (Predicate<Field>) input -> input.getName().equals("delegate")).iterator().next();
//                field.setAccessible(true);
//                try {
//                    FBConnection fbConnection = (FBConnection) field.get((HikariProxyConnection)con);
//                    fbConnection.getTransactionParameters(Connection.TRANSACTION_READ_COMMITTED)
//                            .addArgument(TransactionParameterBuffer.NO_AUTO_UNDO);
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//            }
            con.setAutoCommit(false);
            con = new DbConnection(con);

            localConnection.set((DbConnection) con);
            transactionCaches.set(new HashMap<>());
            transactionBatches.set(new HashMap<>());
            localTimerContext.set(transactionTimer.time());
            return con;
        } catch (Exception e) {
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
        localTimerContext.get().stop();
        localConnection.set(null);
        transactionCaches.get().clear();
        transactionCaches.set(null);
        transactionBatches.get().clear();
        transactionBatches.set(null);
        DbUtils.close(con);
    }

    public static TYPE getDatabaseType() {
        return DATABASE_TYPE;
    }

    public enum TYPE {
        H2,
        MARIADB,
        FIREBIRD;

        public static TYPE getTypeFromJdbcUrl(String jdbcUrl) {
            if (jdbcUrl.contains("jdbc:mysql") || jdbcUrl.contains("jdbc:mariadb"))
                return MARIADB;
            if (jdbcUrl.contains("jdbc:firebirdsql"))
                return FIREBIRD;
            if (jdbcUrl.contains("jdbc:h2"))
                return H2;
            throw new IllegalArgumentException("Unable to determine database type from this: '" + jdbcUrl + "'");
        }
    }

    private static final class DbConnection extends FilteredConnection {

        private DbConnection(Connection con) {
            super(MetricsSql.forRegistry(Nxt.metrics).wrap(con));
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            throw new UnsupportedOperationException("Use Db.beginTransaction() to start a new transaction");
        }

        @Override
        public void commit() throws SQLException {
            if (localConnection.get() == null) {
                super.commit();
                commitCounter.inc();
            } else if (!this.equals(localConnection.get())) {
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
                rollbackCounter.inc();
            } else if (!this.equals(localConnection.get())) {
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
            } else if (!this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            }
        }

    }
}
