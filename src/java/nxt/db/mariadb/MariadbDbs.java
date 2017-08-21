package nxt.db.mariadb;

import nxt.TransactionDb;
import nxt.db.BlockDb;
import nxt.db.store.Dbs;

/**
 * Db-Classes are necessary for the instanciation of some stores and have to be handled separately. In the original version these were static
 */
public class MariadbDbs implements Dbs {

    private final BlockDb blockDb;
    private final TransactionDb transactionDb;

    public MariadbDbs() {
        this.blockDb = new MariadbBlockDB();
        this.transactionDb = new MariadbTransactionDb();
    }

    @Override
    public BlockDb getBlockDb() {
        return blockDb;
    }

    @Override
    public TransactionDb getTransactionDb() {
        return transactionDb;
    }
}
