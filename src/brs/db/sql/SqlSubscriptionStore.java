package brs.db.sql;

import brs.Subscription;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.SubscriptionStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SqlSubscriptionStore implements SubscriptionStore {

  private final BurstKey.LongKeyFactory<Subscription> subscriptionDbKeyFactory = new DbKey.LongKeyFactory<Subscription>("id") {
      @Override
      public BurstKey newKey(Subscription subscription) {
        return subscription.dbKey;
      }
    };

  private final VersionedEntityTable<Subscription> subscriptionTable =
      new VersionedEntitySqlTable<Subscription>("subscription", subscriptionDbKeyFactory) {
        @Override
        protected Subscription load(Connection con, ResultSet rs) throws SQLException {
          return new SqlSubscription(rs);
        }

        @Override
        protected void save(Connection con, Subscription subscription) throws SQLException {
          saveSubscription(con, subscription);
        }

        @Override
        protected String defaultSort() {
          return " ORDER BY time_next ASC, id ASC ";
        }
      };

  private static DbClause getByParticipantClause(final long id) {
    return new DbClause(" (sender_id = ? OR recipient_id = ?) ") {
      @Override
      public int set(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index++, id);
        pstmt.setLong(index++, id);
        return index;
      }
    };
  }

  private static DbClause getUpdateOnBlockClause(final int timestamp) {
    return new DbClause(" time_next <= ? ") {
      @Override
      public int set(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setInt(index++, timestamp);
        return index;
      }
    };
  }

  @Override
  public BurstKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory() {
    return subscriptionDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Subscription> getSubscriptionTable() {
    return subscriptionTable;
  }

  @Override
  public BurstIterator<Subscription> getSubscriptionsByParticipant(Long accountId) {
    return subscriptionTable.getManyBy(getByParticipantClause(accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getIdSubscriptions(Long accountId) {
    return subscriptionTable.getManyBy(new DbClause.LongClause("sender_id", accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getSubscriptionsToId(Long accountId) {
    return subscriptionTable.getManyBy(new DbClause.LongClause("recipient_id", accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getUpdateSubscriptions(int timestamp) {
    return subscriptionTable.getManyBy(getUpdateOnBlockClause(timestamp), 0, -1);
  }

  protected abstract void saveSubscription(Connection con, Subscription subscription) throws SQLException;

  private class SqlSubscription extends Subscription {
    public SqlSubscription(ResultSet rs) throws SQLException {
      super(
            rs.getLong("sender_id"),
            rs.getLong("recipient_id"),
            rs.getLong("id"),
            rs.getLong("amount"),
            rs.getInt("frequency"),
            rs.getInt("time_next"),
            subscriptionDbKeyFactory.newKey(rs.getLong("id"))

            );
    }


  }
}
