package nxt.db.store;

import nxt.TransactionDb;
import nxt.db.BlockDb;
import nxt.db.PeerDb;

public interface Dbs {
    BlockDb getBlockDb();
    TransactionDb getTransactionDb();

    PeerDb getPeerDb();
}
