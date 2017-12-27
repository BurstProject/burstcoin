package brs.db.sql;

import brs.*;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.store.TransactionProcessorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static brs.schema.Tables.UNCONFIRMED_TRANSACTION;
import org.jooq.DSLContext;

public class SqlTransactionProcessorStore implements TransactionProcessorStore {

  private static final Logger logger = LoggerFactory.getLogger(SqlTransactionProcessorStore.class);

  protected final DbKey.LongKeyFactory<TransactionImpl> unconfirmedTransactionDbKeyFactory = new DbKey.LongKeyFactory<TransactionImpl>("id") {

      @Override
      public BurstKey newKey(TransactionImpl transaction) {
        return transaction.getDbKey();
      }

    };
  private final Set<TransactionImpl> lostTransactions = new HashSet<>();
  private final Map<Long, Integer> lostTransactionHeights = new HashMap<>();


  private final EntitySqlTable<TransactionImpl> unconfirmedTransactionTable =
    new EntitySqlTable<TransactionImpl>("unconfirmed_transaction", brs.schema.Tables.UNCONFIRMED_TRANSACTION, unconfirmedTransactionDbKeyFactory) {

        @Override
        protected TransactionImpl load(DSLContext ctx, ResultSet rs) throws SQLException {
          byte[] transactionBytes = rs.getBytes("transaction_bytes");
          try {
            TransactionImpl transaction = TransactionImpl.parseTransaction(transactionBytes);
            transaction.setHeight(rs.getInt("transaction_height"));
            return transaction;
          } catch (BurstException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
          }
        }

        @Override
        protected void save(DSLContext ctx, TransactionImpl transaction) throws SQLException {
          ctx.insertInto(
            UNCONFIRMED_TRANSACTION,
            UNCONFIRMED_TRANSACTION.ID, UNCONFIRMED_TRANSACTION.TRANSACTION_HEIGHT, UNCONFIRMED_TRANSACTION.FEE_PER_BYTE,
            UNCONFIRMED_TRANSACTION.TIMESTAMP, UNCONFIRMED_TRANSACTION.EXPIRATION, UNCONFIRMED_TRANSACTION.TRANSACTION_BYTES,
            UNCONFIRMED_TRANSACTION.HEIGHT
          ).values(
            transaction.getId(), transaction.getHeight(), transaction.getFeeNQT() / transaction.getSize(),
            transaction.getTimestamp(), transaction.getExpiration(), transaction.getBytes(),
            Burst.getBlockchain().getHeight()
          );
        }

        @Override
        public void rollback(int height) {
          List<TransactionImpl> transactions = new ArrayList<>();
          try ( DSLContext ctx = Db.getDSLContext() ) {
            try ( ResultSet rs = ctx.selectFrom(UNCONFIRMED_TRANSACTION).where(UNCONFIRMED_TRANSACTION.HEIGHT.gt(height)).fetchResultSet() ) {
              while (rs.next()) {
                transactions.add(load(ctx, rs));
              }
            }
          }
          catch (SQLException e) {
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
  public BurstIterator<TransactionImpl> getExpiredTransactions() {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return unconfirmedTransactionTable.getManyBy(
        ctx,
        ctx.selectFrom(UNCONFIRMED_TRANSACTION).where(UNCONFIRMED_TRANSACTION.EXPIRATION.lt(Burst.getEpochTime())).getQuery(),
        true
      );
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
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
