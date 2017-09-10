package nxt.db.firebird;

import nxt.TransactionDb;
import nxt.db.BlockDb;
import nxt.db.PeerDb;
import nxt.db.sql.Db;
import nxt.db.store.Dbs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Db-Classes are necessary for the instanciation of some stores and have to be handled separately. In the original version these were static
 */
public class FirebirdDbs implements Dbs {

    private final BlockDb blockDb;
    private final TransactionDb transactionDb;
    private final PeerDb peerDb;


    public FirebirdDbs() {
        FirebirdDbVersion.init();
        this.blockDb = new FirebirdBlockDB();
        this.transactionDb = new FirebirdTransactionDb();
        this.peerDb = new FirebirdPeerDb();
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
        apply("ALTER TABLE transaction DROP CONSTRAINT constraint_ff;");
        apply("ALTER TABLE block DROP CONSTRAINT constraint_3c5;");
        apply("ALTER TABLE block DROP CONSTRAINT constraint_3c;");
    }

    @Override
    public void enableForeignKeyChecks(Connection con) throws SQLException {
        apply("ALTER TABLE transaction ADD CONSTRAINT constraint_ff FOREIGN KEY(block_id) REFERENCES block(id) ON DELETE CASCADE;");
        apply("ALTER TABLE block ADD CONSTRAINT constraint_3c5 FOREIGN KEY(next_block_id) REFERENCES block(id) ON DELETE SET NULL;");
        apply("ALTER TABLE block ADD CONSTRAINT constraint_3c FOREIGN KEY(previous_block_id) REFERENCES block(id) ON DELETE CASCADE;");

    }

    private static void apply(String sql) {
        try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
            try {
                if (sql != null) {
                    stmt.executeUpdate(sql);
                }
            } catch (Exception e) {
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error executing " + sql, e);
        }
    }

    public static String maybeToShortIdentifier(String identifier)
    {
        return FirebirdDbVersion.maybeToShortIdentifier(identifier);
    }
}
