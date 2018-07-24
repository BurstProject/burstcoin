package brs.db.sql;

import brs.Subscription;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.DerivedTableManager;
import brs.db.store.SubscriptionStore;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.SortField;
import org.jooq.Field;

import static brs.schema.Tables.SUBSCRIPTION;

public class SqlSubscriptionStore implements SubscriptionStore {

  private final BurstKey.LongKeyFactory<Subscription> subscriptionDbKeyFactory = new DbKey.LongKeyFactory<Subscription>("id") {
      @Override
      public BurstKey newKey(Subscription subscription) {
        return subscription.dbKey;
      }
    };

  private final VersionedEntityTable<Subscription> subscriptionTable;

  public SqlSubscriptionStore(DerivedTableManager derivedTableManager) {
    subscriptionTable = new VersionedEntitySqlTable<Subscription>("subscription", brs.schema.Tables.SUBSCRIPTION, subscriptionDbKeyFactory, derivedTableManager) {
      @Override
      protected Subscription load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlSubscription(rs);
      }

      @Override
      protected void save(DSLContext ctx, Subscription subscription) throws SQLException {
        saveSubscription(ctx, subscription);
      }

      @Override
      protected List<SortField> defaultSort() {
        List<SortField> sort = new ArrayList<>();
        sort.add(tableClass.field("time_next", Integer.class).asc());
        sort.add(tableClass.field("id", Long.class).asc());
        return sort;
      }
    };
  }

  private static Condition getByParticipantClause(final long id) {
    return SUBSCRIPTION.SENDER_ID.eq(id).or(SUBSCRIPTION.RECIPIENT_ID.eq(id));
  }

  private static Condition getUpdateOnBlockClause(final int timestamp) {
    return SUBSCRIPTION.TIME_NEXT.le(timestamp);
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
    return subscriptionTable.getManyBy(SUBSCRIPTION.SENDER_ID.eq(accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getSubscriptionsToId(Long accountId) {
    return subscriptionTable.getManyBy(SUBSCRIPTION.RECIPIENT_ID.eq(accountId), 0, -1);
  }

  @Override
  public BurstIterator<Subscription> getUpdateSubscriptions(int timestamp) {
    return subscriptionTable.getManyBy(getUpdateOnBlockClause(timestamp), 0, -1);
  }

  protected void saveSubscription(DSLContext ctx, Subscription subscription) throws SQLException {
    brs.schema.tables.records.SubscriptionRecord subscriptionRecord = ctx.newRecord(SUBSCRIPTION);
    subscriptionRecord.setId(subscription.id);
    subscriptionRecord.setSenderId(subscription.senderId);
    subscriptionRecord.setRecipientId(subscription.recipientId);
    subscriptionRecord.setAmount(subscription.amountNQT);
    subscriptionRecord.setFrequency(subscription.frequency);
    subscriptionRecord.setTimeNext(subscription.getTimeNext());
    subscriptionRecord.setHeight(brs.Burst.getBlockchain().getHeight());
    subscriptionRecord.setLatest(true);
    DbUtils.mergeInto(
      ctx, subscriptionRecord, SUBSCRIPTION,
      (
        new Field[] {
          subscriptionRecord.field("id"),
          subscriptionRecord.field("sender_id"),
          subscriptionRecord.field("recipient_id"),
          subscriptionRecord.field("amount"),
          subscriptionRecord.field("frequency"),
          subscriptionRecord.field("time_next"),
          subscriptionRecord.field("height"),
          subscriptionRecord.field("latest")
        }
       )
    );
  }

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
