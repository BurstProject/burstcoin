package nxt.db.firebird;

import nxt.Nxt;
import nxt.NxtException;
import nxt.TransactionImpl;
import nxt.db.sql.Db;
import nxt.db.sql.EntitySqlTable;
import nxt.db.sql.SqlTransactionProcessorStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class FirebirdTransactionProcessorStore extends SqlTransactionProcessorStore {


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
                            + "fee_per_byte, \"timestamp\", expiration, transaction_bytes, height) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        int i = 0;
                        pstmt.setLong(++i, transaction.getId());
                        pstmt.setInt(++i, transaction.getHeight());
                        pstmt.setLong(++i, transaction.getFeeNQT() / transaction.getSize());
                        pstmt.setInt(++i, transaction.getTimestamp());
                        pstmt.setInt(++i, transaction.getExpiration());
                        pstmt.setBytes(++i, transaction.getBytes());
                        pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
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


    @Override
    public EntitySqlTable<TransactionImpl> getUnconfirmedTransactionTable() {
        return unconfirmedTransactionTable;
    }
}
