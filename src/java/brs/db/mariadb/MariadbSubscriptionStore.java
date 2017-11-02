package brs.db.mariadb;

import brs.Burst;
import brs.Subscription;
import brs.db.sql.SqlSubscriptionStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class MariadbSubscriptionStore extends SqlSubscriptionStore {
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
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }
}
