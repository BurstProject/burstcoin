package nxt.db.sql;

import nxt.Nxt;
import nxt.Subscription;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;
import nxt.db.store.SubscriptionStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SqlSubscriptionStore implements SubscriptionStore {

    private final NxtKey.LongKeyFactory<Subscription> subscriptionDbKeyFactory = new DbKey.LongKeyFactory<Subscription>("id") {
        @Override
        public NxtKey newKey(Subscription subscription) {
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
    public NxtKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory() {
        return subscriptionDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<Subscription> getSubscriptionTable() {
        return subscriptionTable;
    }

    @Override
    public NxtIterator<Subscription> getSubscriptionsByParticipant(Long accountId) {
        return subscriptionTable.getManyBy(getByParticipantClause(accountId), 0, -1);
    }

    @Override
    public NxtIterator<Subscription> getIdSubscriptions(Long accountId) {
        return subscriptionTable.getManyBy(new DbClause.LongClause("sender_id", accountId), 0, -1);
    }

    @Override
    public NxtIterator<Subscription> getSubscriptionsToId(Long accountId) {
        return subscriptionTable.getManyBy(new DbClause.LongClause("recipient_id", accountId), 0, -1);
    }

    @Override
    public NxtIterator<Subscription> getUpdateSubscriptions(int timestamp) {
        return subscriptionTable.getManyBy(getUpdateOnBlockClause(timestamp), 0, -1);
    }

    private void saveSubscription(Connection con, Subscription subscription) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO subscription (id, "
                + "sender_id, recipient_id, amount, frequency, time_next, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, subscription.id);
            pstmt.setLong(++i, subscription.senderId);
            pstmt.setLong(++i, subscription.recipientId);
            pstmt.setLong(++i, subscription.amountNQT);
            pstmt.setInt(++i, subscription.frequency);
            pstmt.setInt(++i, subscription.timeNext);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
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
