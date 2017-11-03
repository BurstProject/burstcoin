package brs.db.mariadb;

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
public class MariadbDbs implements Dbs {

    private final BlockDb blockDb;
    private final TransactionDb transactionDb;
    private final PeerDb peerDb;


    public MariadbDbs() {
        MariadbDbVersion.init();
        this.blockDb = new MariadbBlockDB();
        this.transactionDb = new MariadbTransactionDb();
        this.peerDb = new MariadbPeerDb();
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
        Statement stmt = con.createStatement();
        stmt.executeUpdate("SET FOREIGN_KEY_CHECKS=0;");
        stmt.executeUpdate("SET unique_checks=0;");


    }

    @Override
    public void enableForeignKeyChecks(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.executeUpdate("SET FOREIGN_KEY_CHECKS=1;");
        stmt.executeUpdate("SET unique_checks=1;");
    }
}
