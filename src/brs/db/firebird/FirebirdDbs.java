package brs.db.firebird;

import brs.TransactionDb;
import brs.db.BlockDb;
import brs.db.PeerDb;
import brs.db.sql.Db;
import brs.db.store.Dbs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Arrays;
import java.util.List;

import java.sql.ResultSet;

import brs.db.sql.SqlBlockDb;
import brs.db.sql.SqlTransactionDb;
import brs.db.sql.SqlPeerDb;

/**
 * Db-Classes are necessary for the instanciation of some stores and have to be handled separately. In the original version these were static
 */
public class FirebirdDbs implements Dbs {

  private final BlockDb blockDb;
  private final TransactionDb transactionDb;
  private final PeerDb peerDb;


  public FirebirdDbs() {
    this.blockDb       = new SqlBlockDb();
    this.transactionDb = new SqlTransactionDb();
    this.peerDb        = new SqlPeerDb();
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

}
