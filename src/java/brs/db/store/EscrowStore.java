package brs.db.store;

import brs.Block;
import brs.Escrow;
import brs.TransactionImpl;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.VersionedEntityTable;
import brs.db.sql.DbKey;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public interface EscrowStore {

    NxtKey.LongKeyFactory<Escrow> getEscrowDbKeyFactory();

    VersionedEntityTable<Escrow> getEscrowTable();

    DbKey.LinkKeyFactory<Escrow.Decision> getDecisionDbKeyFactory();

    VersionedEntityTable<Escrow.Decision> getDecisionTable();

    Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId);

    void updateOnBlock(Block block);

    List<TransactionImpl> getResultTransactions();

    ConcurrentSkipListSet<Long> getUpdatedEscrowIds();

    NxtIterator<Escrow.Decision> getDecisions(Long id);
}
