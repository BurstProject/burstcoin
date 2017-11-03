package brs.db.sql;

import com.github.gquintana.metrics.sql.MetricsSql;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariProxyConnection;
import com.zaxxer.hikari.pool.ProxyConnection;

import brs.Constants;
import brs.Burst;

import com.google.common.base.Predicate;
import org.reflections.ReflectionUtils;
import java.lang.reflect.Field;

import org.firebirdsql.jdbc.FBConnection;
import org.firebirdsql.management.FBManager;
import org.firebirdsql.gds.TransactionParameterBuffer;
import org.firebirdsql.gds.impl.GDSType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public final class Db {

  private static final Logger logger = LoggerFactory.getLogger(Db.class);

  private static final HikariDataSource cp;
  private static final ThreadLocal<DbConnection> localConnection = new ThreadLocal<>();
  private static final ThreadLocal<Map<String, Map<DbKey, Object>>> transactionCaches = new ThreadLocal<>();
  private static final ThreadLocal<Map<String, Map<DbKey, Object>>> transactionBatches = new ThreadLocal<>();
  private static final TYPE DATABASE_TYPE;
  private static final boolean enableSqlMetrics = Burst.getBooleanProperty("brs.enableSqlMetrics", false);


  static {
    String dbUrl;
    String dbUsername;
    String dbPassword;

    if (Constants.isTestnet) {
      dbUrl = Burst.getStringProperty("brs.testDbUrl");
      dbUsername = Burst.getStringProperty("brs.testDbUsername");
      dbPassword = Burst.getStringProperty("brs.testDbPassword");
    } else {
      dbUrl = Burst.getStringProperty("brs.dbUrl");
      dbUsername = Burst.getStringProperty("brs.dbUsername");
      dbPassword = Burst.getStringProperty("brs.dbPassword");
    }

    logger.debug("Database jdbc url set to: " + dbUrl);
    try {
      DATABASE_TYPE = TYPE.getTypeFromJdbcUrl(dbUrl);
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      if (dbUsername != null)
        config.setUsername(dbUsername);
      if (dbPassword != null)
        config.setPassword(dbPassword);

      config.setMaximumPoolSize(Burst.getIntProperty("brs.dbMaximumPoolSize"));

      switch (DATABASE_TYPE) {
        case MARIADB:
          config.setAutoCommit(false);
          config.addDataSourceProperty("cachePrepStmts", "true");
          config.addDataSourceProperty("prepStmtCacheSize", "250");
          config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
          config.addDataSourceProperty("characterEncoding", "utf8mb4");
          config.addDataSourceProperty("useUnicode", "true");
          config.addDataSourceProperty("useServerPrepStmts", "false");
          config.addDataSourceProperty("rewriteBatchedStatements", "true");
          config.setConnectionInitSql("SET NAMES utf8mb4;");
          break;
        case FIREBIRD:
          String jnaPath = System.getProperty("jna.library.path");
          if ( jnaPath == null || jnaPath.isEmpty() ) {
            Path path = Paths.get(
                                  "lib/firebird/"
                                  + ( System.getProperty("sun.arch.data.model") == "32" ? "32" : "64" )
                                  + "/"
                                  ).toAbsolutePath();
            System.setProperty("jna.library.path", path.toString());
            logger.info("Set jna.library.path to: " + path.toString());
          }
          Class.forName("org.firebirdsql.jdbc.FBDriver");

          config.setAutoCommit(false);
          config.addDataSourceProperty("encoding", "UTF8");
          config.addDataSourceProperty("cachePrepStmts", "true");
          config.addDataSourceProperty("prepStmtCacheSize", "250");
          config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
          config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

          if (dbUrl.startsWith("jdbc:firebirdsql:embedded:")) {
            String firebirdDb = dbUrl.replaceFirst("^jdbc:firebirdsql:embedded:", "").replaceFirst("\\?.*$", "");

            if (!new File(firebirdDb).isFile()) {
              FBManager manager = new FBManager(GDSType.getType("EMBEDDED"));
              manager.start();
              manager.createDatabase(
                                     firebirdDb,
                                     ( dbUsername != null ? dbUsername : "" ),
                                     ( dbPassword != null ? dbPassword : "" )
                                     );
              manager.stop();
            }
          }

          break;
        case H2:
          Class.forName("org.h2.Driver");
          config.setAutoCommit(false);
          config.addDataSourceProperty("cachePrepStmts", "true");
          config.addDataSourceProperty("prepStmtCacheSize", "250");
          config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
          break;
      }

      cp = new HikariDataSource(config);

      if (DATABASE_TYPE == TYPE.H2) {
        int defaultLockTimeout = Burst.getIntProperty("brs.dbDefaultLockTimeout") * 1000;
        try (Connection con = cp.getConnection();
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
      try (Connection con = cp.getConnection();
           Statement stmt = con.createStatement()) {
        stmt.execute("ANALYZE SAMPLE_SIZE 0");
      } catch (SQLException e) {
        throw new RuntimeException(e.toString(), e);
      }
    }
  }

  public static void shutdown() {
    try {
      Connection con = cp.getConnection();
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
    Connection con = cp.getConnection();
    return con;
  }

  public static Connection getConnection() throws SQLException {
    Connection con = localConnection.get();
    if (con != null) {
      return con;
    }
    con = getPooledConnection();
    con.setAutoCommit(true);
    if (enableSqlMetrics)
      return new MeteredDbConnection(con);
    else
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
      if (DATABASE_TYPE == TYPE.FIREBIRD) {
        Field field = ReflectionUtils.getFields(ProxyConnection.class, (Predicate<Field>) input -> input.getName().equals("delegate")).iterator().next();
        field.setAccessible(true);
        try {
          FBConnection fbConnection = (FBConnection) field.get((HikariProxyConnection)con);
          fbConnection.getTransactionParameters(Connection.TRANSACTION_READ_COMMITTED)
              .addArgument(TransactionParameterBuffer.NO_AUTO_UNDO);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
      con.setAutoCommit(false);
      if (enableSqlMetrics)
        con = new MeteredDbConnection(con);
      else
        con = new DbConnection(con);

      localConnection.set((DbConnection) con);
      transactionCaches.set(new HashMap<>());
      transactionBatches.set(new HashMap<>());
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

  private static class MeteredDbConnection extends DbConnection {

    private MeteredDbConnection(Connection con) {
      super(MetricsSql.forRegistry(Burst.metrics).wrap(con));
    }
  }

  private static class DbConnection extends FilteredConnection {

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
