package nxt.db.store;

import nxt.TransactionDb;
import nxt.db.BlockDb;

public interface Dbs {
    BlockDb getBlockDb();
    TransactionDb getTransactionDb();
}
