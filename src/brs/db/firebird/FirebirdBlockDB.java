package brs.db.firebird;

import brs.BlockImpl;
import brs.Burst;
import brs.BurstException;
import brs.db.sql.Db;
import brs.db.sql.DbUtils;
import brs.db.sql.SqlBlockDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

class FirebirdBlockDB extends SqlBlockDb {
  private static final Logger logger = LoggerFactory.getLogger(FirebirdBlockDB.class);

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
