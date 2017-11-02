package brs.db.store;

import brs.Transaction;
import brs.TransactionImpl;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.sql.EntitySqlTable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface TransactionProcessorStore {
    // WATCH: BUSINESS-LOGIC
    void processLater(Collection<TransactionImpl> transactions);

    NxtKey.LongKeyFactory<TransactionImpl> getUnconfirmedTransactionDbKeyFactory();

    Set<TransactionImpl> getLostTransactions();

    Map<Long, Integer> getLostTransactionHeights();

    EntitySqlTable<TransactionImpl> getUnconfirmedTransactionTable();

    NxtIterator<TransactionImpl> getExpiredTransactions();

    int deleteTransaction (Transaction transaction);
}
