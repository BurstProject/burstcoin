package brs.db.store;

import brs.TransactionDb;
import brs.db.BlockDb;
import brs.db.PeerDb;

public interface Dbs {

  BlockDb getBlockDb();

  TransactionDb getTransactionDb();

  PeerDb getPeerDb();

}
