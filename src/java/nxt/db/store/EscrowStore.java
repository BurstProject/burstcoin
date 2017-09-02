package nxt.db.store;

import nxt.Block;
import nxt.Escrow;
import nxt.TransactionImpl;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;
import nxt.db.sql.DbKey;

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
