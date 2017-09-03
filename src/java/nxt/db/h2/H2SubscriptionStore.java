package nxt.db.h2;

import nxt.Nxt;
import nxt.Subscription;
import nxt.db.sql.SqlSubscriptionStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class H2SubscriptionStore extends SqlSubscriptionStore {
    @Override
    protected void saveSubscription(Connection con, Subscription subscription) throws SQLException {
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
}
