package nxt.db.firebird;

import nxt.Trade;
import nxt.db.sql.SqlTradeStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class FirebirdTradeStore extends SqlTradeStore {

    protected void saveTrade(Connection con, Trade trade) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO trade (asset_id, block_id, "
                + "ask_order_id, bid_order_id, ask_order_height, bid_order_height, seller_id, buyer_id, quantity, price, \"timestamp\", height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, trade.getAssetId());
            pstmt.setLong(++i, trade.getBlockId());
            pstmt.setLong(++i, trade.getAskOrderId());
            pstmt.setLong(++i, trade.getBidOrderId());
            pstmt.setInt(++i, trade.getAskOrderHeight());
            pstmt.setInt(++i, trade.getBidOrderHeight());
            pstmt.setLong(++i, trade.getSellerId());
            pstmt.setLong(++i, trade.getBuyerId());
            pstmt.setLong(++i, trade.getQuantityQNT());
            pstmt.setLong(++i, trade.getPriceNQT());
            pstmt.setInt(++i, trade.getTimestamp());
            pstmt.setInt(++i, trade.getHeight());
            pstmt.executeUpdate();
        }
    }
}
