package nxt.db.store;

import nxt.Transaction;
import nxt.TransactionImpl;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.sql.EntitySqlTable;

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
