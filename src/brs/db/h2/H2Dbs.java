package brs.db.h2;

import brs.TransactionDb;
import brs.db.BlockDb;
import brs.db.PeerDb;
import brs.db.store.Dbs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Db-Classes are necessary for the instanciation of some stores and have to be handled separately. In the original version these were static
 */
public class H2Dbs implements Dbs {

  private final BlockDb blockDb;
  private final TransactionDb transactionDb;
  private final PeerDb peerDb;


  public H2Dbs() {
    H2DbVersion.init();
    this.blockDb = new H2BlockDB();
    this.transactionDb = new H2TransactionDb();
    this.peerDb = new H2PeerDb();
  }

  @Override
  public BlockDb getBlockDb() {
    return blockDb;
  }

  @Override
  public TransactionDb getTransactionDb() {
    return transactionDb;
  }

  @Override
  public PeerDb getPeerDb() {
    return peerDb;
  }

  @Override
  public void disableForeignKeyChecks(Connection con) throws SQLException {
    try ( Statement stmt = con.createStatement() ) {
      stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
    }
  }

  @Override
  public void enableForeignKeyChecks(Connection con) throws SQLException {
    try ( Statement stmt = con.createStatement() ) {
      stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
    }
  }
}
