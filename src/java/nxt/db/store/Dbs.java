package nxt.db.store;

import nxt.TransactionDb;
import nxt.db.BlockDb;
import nxt.db.PeerDb;

import java.sql.Connection;
import java.sql.SQLException;

public interface Dbs {
    BlockDb getBlockDb();
    TransactionDb getTransactionDb();

    PeerDb getPeerDb();

    void disableForeignKeyChecks(Connection con) throws SQLException;

    void enableForeignKeyChecks(Connection con) throws SQLException;
}
