package brs.db.derby;

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
public class DerbyDbs implements Dbs {

  private final BlockDb blockDb;
  private final TransactionDb transactionDb;
  private final PeerDb peerDb;


  public DerbyDbs() {
    DerbyDbVersion.init();
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

  @Override
  public void disableForeignKeyChecks(Connection con) throws SQLException {
    // This is a very very ugly and dangerous operation
    apply("ALTER TABLE \"transaction\" DROP CONSTRAINT \"constraint_ff\";");
    apply("ALTER TABLE \"block\" DROP CONSTRAINT \"constraint_3c5\";");
    apply("ALTER TABLE \"block\" DROP CONSTRAINT \"constraint_3c\";");

  }

  @Override
  public void enableForeignKeyChecks(Connection con) throws SQLException {
    apply("ALTER TABLE \"transaction\" ADD CONSTRAINT \"constraint_ff\" FOREIGN KEY(\"block_id\") REFERENCES \"block\"(\"id\") ON DELETE CASCADE;");
    apply("ALTER TABLE \"block\" ADD CONSTRAINT \"constraint_3c5\" FOREIGN KEY(\"next_block_id\") REFERENCES \"block\"(\"id\") ON DELETE SET NULL;");
    apply("ALTER TABLE \"block\" ADD CONSTRAINT \"constraint_3c\" FOREIGN KEY(\"previous_block_id\") REFERENCES \"block\"(\"id\") ON DELETE CASCADE;");

    // convert array to list
    List<String> lTables = Arrays.asList(
                                         (
                                          new String[] {
                                            "block", "transaction", "alias", "alias_offer", "asset", "trade", "ask_order",
                                            "bid_order", "goods", "purchase", "account", "account_asset", "purchase_feedback",
                                            "purchase_public_feedback", "unconfirmed_transaction", "asset_transfer",
                                            "reward_recip_assign", "escrow", "escrow_decision", "subscription", "\"AT\"", "at_state"
                                          }
                                          )
                                         );

    for (String table : lTables) {
      Long maxValue = (long) 0;
      try ( Statement stmt = con.createStatement() ) {
        try ( ResultSet rs = stmt.executeQuery("SELECT MAX(\"db_id\") FROM \"" + table + "\"") ) {
          rs.next();
          maxValue = rs.getLong(1);
          Db.commitTransaction();
        }
        catch (SQLException e) {
          throw new RuntimeException("Database error executing", e);
        }
        if ( maxValue > 0 ) {
          apply("ALTER TABLE \"" + table + "\" ALTER COLUMN \"db_id\" RESTART WITH " + maxValue);
        }
      }
    }
  }

  private static void apply(String sql) {
    try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
      try {
        if (sql != null) {
          stmt.executeUpdate(sql);
        }
      } catch (SQLException e) {
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Database error executing " + sql, e);
    }
  }

  public static String maybeToShortIdentifier(String identifier)
  {
    return DerbyDbVersion.maybeToShortIdentifier(identifier);
  }
}
