package brs.db.sql;

import brs.*;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.store.TransactionProcessorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SqlTransactionProcessorStore implements TransactionProcessorStore {

    private static final Logger logger = LoggerFactory.getLogger(SqlTransactionProcessorStore.class);

    protected final DbKey.LongKeyFactory<TransactionImpl> unconfirmedTransactionDbKeyFactory = new DbKey.LongKeyFactory<TransactionImpl>("id") {

        @Override
        public NxtKey newKey(TransactionImpl transaction) {
            return transaction.getDbKey();
        }

    };
    private final Set<TransactionImpl> lostTransactions = new HashSet<>();
    private final Map<Long, Integer> lostTransactionHeights = new HashMap<>();


    private final EntitySqlTable<TransactionImpl> unconfirmedTransactionTable =
            new EntitySqlTable<TransactionImpl>("unconfirmed_transaction", unconfirmedTransactionDbKeyFactory) {

                @Override
                protected TransactionImpl load(Connection con, ResultSet rs) throws SQLException {
                    byte[] transactionBytes = rs.getBytes("transaction_bytes");
                    try {
                        TransactionImpl transaction = TransactionImpl.parseTransaction(transactionBytes);
                        transaction.setHeight(rs.getInt("transaction_height"));
                        return transaction;
                    } catch (NxtException.ValidationException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }

                @Override
                protected void save(Connection con, TransactionImpl transaction) throws SQLException {
                    try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO unconfirmed_transaction (id, transaction_height, "
                            + "fee_per_byte, timestamp, expiration, transaction_bytes, height) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        int i = 0;
                        pstmt.setLong(++i, transaction.getId());
                        pstmt.setInt(++i, transaction.getHeight());
                        pstmt.setLong(++i, transaction.getFeeNQT() / transaction.getSize());
                        pstmt.setInt(++i, transaction.getTimestamp());
                        pstmt.setInt(++i, transaction.getExpiration());
                        pstmt.setBytes(++i, transaction.getBytes());
                        pstmt.setInt(++i, Burst.getBlockchain().getHeight());
                        pstmt.executeUpdate();
                    }
                }

                @Override
                public void rollback(int height) {
                    List<TransactionImpl> transactions = new ArrayList<>();
                    try (Connection con = Db.getConnection();
                         PreparedStatement pstmt = con.prepareStatement("SELECT * FROM unconfirmed_transaction WHERE height > ?")) {
                        pstmt.setInt(1, height);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                transactions.add(load(con, rs));
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                    super.rollback(height);
                    // WATCH: BUSINESS-LOGIC
                    processLater(transactions);
                }

                @Override
                protected String defaultSort() {
                    return " ORDER BY transaction_height ASC, fee_per_byte DESC, timestamp ASC, id ASC ";
                }
            };

    private final DbClause expiredClause = new DbClause(" expiration < ? ") {
        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setInt(index, Burst.getEpochTime());
            return index + 1;
        }
    };

    // WATCH: BUSINESS-LOGIC
    @Override
    public void processLater(Collection<TransactionImpl> transactions) {
        synchronized (BlockchainImpl.getInstance()) {
            for (TransactionImpl transaction : transactions) {
                lostTransactions.add(transaction);
            }
        }
    }

    @Override
    public DbKey.LongKeyFactory<TransactionImpl> getUnconfirmedTransactionDbKeyFactory() {
        return unconfirmedTransactionDbKeyFactory;
    }

    @Override
    public Set<TransactionImpl> getLostTransactions() {
        return lostTransactions;
    }

    @Override
    public Map<Long, Integer> getLostTransactionHeights() {
        return lostTransactionHeights;
    }

    @Override
    public EntitySqlTable<TransactionImpl> getUnconfirmedTransactionTable() {
        return unconfirmedTransactionTable;
    }

    @Override
    public NxtIterator<TransactionImpl> getExpiredTransactions() {
        return unconfirmedTransactionTable.getManyBy(expiredClause, 0, -1, "");
    }

    @Override
    public int deleteTransaction(Transaction transaction) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM unconfirmed_transaction WHERE id = ?")) {
            pstmt.setLong(1, transaction.getId());
            int deleted = pstmt.executeUpdate();
            return deleted;
        } catch (SQLException e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e.toString(), e);
        }
    }
}
