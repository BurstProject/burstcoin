package nxt.db.firebird;

import nxt.TransactionDb;
import nxt.db.BlockDb;
import nxt.db.PeerDb;
import nxt.db.store.Dbs;

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
}
