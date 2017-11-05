package brs.db.store;

import brs.Block;
import brs.Escrow;
import brs.TransactionImpl;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.sql.DbKey;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public interface EscrowStore {

  BurstKey.LongKeyFactory<Escrow> getEscrowDbKeyFactory();

  VersionedEntityTable<Escrow> getEscrowTable();

  DbKey.LinkKeyFactory<Escrow.Decision> getDecisionDbKeyFactory();

  VersionedEntityTable<Escrow.Decision> getDecisionTable();

  Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId);

  void updateOnBlock(Block block);

  List<TransactionImpl> getResultTransactions();

  ConcurrentSkipListSet<Long> getUpdatedEscrowIds();

  BurstIterator<Escrow.Decision> getDecisions(Long id);
}
