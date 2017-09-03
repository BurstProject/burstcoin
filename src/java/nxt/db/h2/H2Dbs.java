package nxt.db.h2;

import nxt.TransactionDb;
import nxt.db.BlockDb;
import nxt.db.PeerDb;
import nxt.db.store.Dbs;

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
}
