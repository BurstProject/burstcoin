package nxt.db.sql;

import nxt.Trade;
import nxt.db.NxtKey;
import nxt.db.store.TradeStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SqlTradeStore implements TradeStore {
    private final DbKey.LinkKeyFactory<Trade> tradeDbKeyFactory = new DbKey.LinkKeyFactory<Trade>("ask_order_id", "bid_order_id") {

        @Override
        public NxtKey newKey(Trade trade) {
            return trade.dbKey;
        }

    };

    private final EntitySqlTable<Trade> tradeTable = new EntitySqlTable<Trade>("trade", tradeDbKeyFactory) {

        @Override
        protected Trade load(Connection con, ResultSet rs) throws SQLException {
            return new SqlTrade(rs);
        }

        @Override
        protected void save(Connection con, Trade trade) throws SQLException {
            saveTrade(con, trade);
        }

    };

    @Override
    public DbIterator<Trade> getAllTrades(int from, int to) {
        return tradeTable.getAll(from, to);
    }

    @Override
    public DbIterator<Trade> getAssetTrades(long assetId, int from, int to) {
        return tradeTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    @Override
    public DbIterator<Trade> getAccountTrades(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE seller_id = ?"
                    + " UNION ALL SELECT * FROM trade WHERE buyer_id = ? AND seller_id <> ? ORDER BY height DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return tradeTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to) {
        Connection con = null;
        try {
            con = Db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE seller_id = ? AND asset_id = ?"
                    + " UNION ALL SELECT * FROM trade WHERE buyer_id = ? AND seller_id <> ? AND asset_id = ? ORDER BY height DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return tradeTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getTradeCount(long assetId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM trade WHERE asset_id = ?")) {
            pstmt.setLong(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void saveTrade(Connection con, Trade trade) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO trade (asset_id, block_id, "
                + "ask_order_id, bid_order_id, ask_order_height, bid_order_height, seller_id, buyer_id, quantity, price, timestamp, height) "
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

    @Override
    public DbKey.LinkKeyFactory<Trade> getTradeDbKeyFactory() {
        return tradeDbKeyFactory;
    }

    @Override
    public EntitySqlTable<Trade> getTradeTable() {
        return tradeTable;
    }

    private class SqlTrade extends Trade {

        private SqlTrade(ResultSet rs) throws SQLException {
            super(
                    rs.getInt("timestamp"),
                    rs.getLong("asset_id"),
                    rs.getLong("block_id"),
                    rs.getInt("height"),
                    rs.getLong("ask_order_id"),
                    rs.getLong("bid_order_id"),
                    rs.getInt("ask_order_height"),
                    rs.getInt("bid_order_height"),
                    rs.getLong("seller_id"),
                    rs.getLong("buyer_id"),
                    tradeDbKeyFactory.newKey(rs.getLong("ask_order_id"), rs.getLong("bid_order_id")),
                    rs.getLong("quantity"),
                    rs.getLong("price")
            );
        }
    }
}
