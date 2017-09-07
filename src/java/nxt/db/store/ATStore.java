package nxt.db.store;

import nxt.AT;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;

public interface ATStore {

    boolean isATAccountId(Long id);

    List<Long> getOrderedATs();

    AT getAT(Long id);

    List<Long> getATsIssuedBy(Long accountId);

    Collection<Long> getAllATIds();

    NxtKey.LongKeyFactory<AT> getAtDbKeyFactory();

    VersionedEntityTable<AT> getAtTable();

    NxtKey.LongKeyFactory<AT.ATState> getAtStateDbKeyFactory();

    VersionedEntityTable<AT.ATState> getAtStateTable();

    Long findTransaction(int startHeight, int endHeight, Long atID, int numOfTx, long minAmount);

    int findTransactionHeight(Long transactionId, int height, Long atID, long minAmount);
}
