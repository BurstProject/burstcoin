package nxt;

import nxt.db.Db;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Trade {

    public static enum Event {
        TRADE
    }

    private static final Listeners<Trade,Event> listeners = new Listeners<>();

    private static final DbKey.LinkKeyFactory<Trade> tradeDbKeyFactory = new DbKey.LinkKeyFactory<Trade>("ask_order_id", "bid_order_id") {

        @Override
        public DbKey newKey(Trade trade) {
            return trade.dbKey;
        }

    };

    private static final EntityDbTable<Trade> tradeTable = new EntityDbTable<Trade>("trade", tradeDbKeyFactory) {

        @Override
        protected Trade load(Connection con, ResultSet rs) throws SQLException {
            return new Trade(rs);
        }

        @Override
        protected void save(Connection con, Trade trade) throws SQLException {
            trade.save(con);
        }

    };

    public static DbIterator<Trade> getAllTrades(int from, int to) {
        return tradeTable.getAll(from, to);
    }

    public static int getCount() {
        return tradeTable.getCount();
    }

    public static boolean addListener(Listener<Trade> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Trade> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<Trade> getAssetTrades(long assetId, int from, int to) {
        return tradeTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    public static DbIterator<Trade> getAccountTrades(long accountId, int from, int to) {
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

    public static DbIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to) {
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

    public static int getTradeCount(long assetId) {
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

    static Trade addTrade(long assetId, Block block, Order.Ask askOrder, Order.Bid bidOrder) {
        Trade trade = new Trade(assetId, block, askOrder, bidOrder);
        tradeTable.insert(trade);
        listeners.notify(trade, Event.TRADE);
        return trade;
    }

    static void init() {}


    private final int timestamp;
    private final long assetId;
    private final long blockId;
    private final int height;
    private final long askOrderId;
    private final long bidOrderId;
    private final int askOrderHeight;
    private final int bidOrderHeight;
    private final long sellerId;
    private final long buyerId;
    private final DbKey dbKey;
    private final long quantityQNT;
    private final long priceNQT;
    private final boolean isBuy;

    private Trade(long assetId, Block block, Order.Ask askOrder, Order.Bid bidOrder) {
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.assetId = assetId;
        this.timestamp = block.getTimestamp();
        this.askOrderId = askOrder.getId();
        this.bidOrderId = bidOrder.getId();
        this.askOrderHeight = askOrder.getHeight();
        this.bidOrderHeight = bidOrder.getHeight();
        this.sellerId = askOrder.getAccountId();
        this.buyerId = bidOrder.getAccountId();
        this.dbKey = tradeDbKeyFactory.newKey(this.askOrderId, this.bidOrderId);
        this.quantityQNT = Math.min(askOrder.getQuantityQNT(), bidOrder.getQuantityQNT());
        this.isBuy = askOrderHeight < bidOrderHeight || (askOrderHeight == bidOrderHeight && askOrderId < bidOrderId);
        this.priceNQT = isBuy ? askOrder.getPriceNQT() : bidOrder.getPriceNQT();
    }

    private Trade(ResultSet rs) throws SQLException {
        this.assetId = rs.getLong("asset_id");
        this.blockId = rs.getLong("block_id");
        this.askOrderId = rs.getLong("ask_order_id");
        this.bidOrderId = rs.getLong("bid_order_id");
        this.askOrderHeight = rs.getInt("ask_order_height");
        this.bidOrderHeight = rs.getInt("bid_order_height");
        this.sellerId = rs.getLong("seller_id");
        this.buyerId = rs.getLong("buyer_id");
        this.dbKey = tradeDbKeyFactory.newKey(this.askOrderId, this.bidOrderId);
        this.quantityQNT = rs.getLong("quantity");
        this.priceNQT = rs.getLong("price");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
        this.isBuy = askOrderHeight < bidOrderHeight || (askOrderHeight == bidOrderHeight && askOrderId < bidOrderId);
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO trade (asset_id, block_id, "
                + "ask_order_id, bid_order_id, ask_order_height, bid_order_height, seller_id, buyer_id, quantity, price, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getAssetId());
            pstmt.setLong(++i, this.getBlockId());
            pstmt.setLong(++i, this.getAskOrderId());
            pstmt.setLong(++i, this.getBidOrderId());
            pstmt.setInt(++i, this.getAskOrderHeight());
            pstmt.setInt(++i, this.getBidOrderHeight());
            pstmt.setLong(++i, this.getSellerId());
            pstmt.setLong(++i, this.getBuyerId());
            pstmt.setLong(++i, this.getQuantityQNT());
            pstmt.setLong(++i, this.getPriceNQT());
            pstmt.setInt(++i, this.getTimestamp());
            pstmt.setInt(++i, this.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getBlockId() { return blockId; }

    public long getAskOrderId() { return askOrderId; }

    public long getBidOrderId() { return bidOrderId; }

    public int getAskOrderHeight() {
        return askOrderHeight;
    }

    public int getBidOrderHeight() {
        return bidOrderHeight;
    }

    public long getSellerId() {
        return sellerId;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public long getQuantityQNT() { return quantityQNT; }

    public long getPriceNQT() { return priceNQT; }
    
    public long getAssetId() { return assetId; }
    
    public int getTimestamp() { return timestamp; }

    public int getHeight() {
        return height;
    }

    public boolean isBuy() {
        return isBuy;
    }

    @Override
    public String toString() {
        return "Trade asset: " + Convert.toUnsignedLong(assetId) + " ask: " + Convert.toUnsignedLong(askOrderId)
                + " bid: " + Convert.toUnsignedLong(bidOrderId) + " price: " + priceNQT + " quantity: " + quantityQNT + " height: " + height;
    }

}
