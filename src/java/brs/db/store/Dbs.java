package brs.db.store;

import brs.TransactionDb;
import brs.db.BlockDb;
import brs.db.PeerDb;

import java.sql.Connection;
import java.sql.SQLException;

public interface Dbs {
    BlockDb getBlockDb();
    TransactionDb getTransactionDb();

    PeerDb getPeerDb();

    void disableForeignKeyChecks(Connection con) throws SQLException;

    void enableForeignKeyChecks(Connection con) throws SQLException;
}
