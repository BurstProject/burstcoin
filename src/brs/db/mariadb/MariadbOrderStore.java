package brs.db.mariadb;

import brs.Burst;
import brs.Order;
import brs.db.sql.SqlOrderStore;
import brs.db.sql.DbUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class MariadbOrderStore extends SqlOrderStore {
  @Override
  protected void saveOrder(Connection con, String table, Order order) throws SQLException {
    try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO "
                                                        + DbUtils.quoteTableName(table)
                                                        + " (id, account_id, asset_id, "
                                                        + "price, quantity, creation_height, height, latest) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
      int i = 0;
      pstmt.setLong(++i, order.getId());
      pstmt.setLong(++i, order.getAccountId());
      pstmt.setLong(++i, order.getAssetId());
      pstmt.setLong(++i, order.getPriceNQT());
      pstmt.setLong(++i, order.getQuantityQNT());
      pstmt.setInt(++i, order.getHeight());
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }
}
