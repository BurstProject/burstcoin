package nxt;

import nxt.db.Db;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class Order {

    private static void matchOrders(long assetId) {

        Order.Ask askOrder;
        Order.Bid bidOrder;

        while ((askOrder = Ask.getNextOrder(assetId)) != null
                && (bidOrder = Bid.getNextOrder(assetId)) != null) {

            if (askOrder.getPriceNQT() > bidOrder.getPriceNQT()) {
                break;
            }


            Trade trade = Trade.addTrade(assetId, Nxt.getBlockchain().getLastBlock(), askOrder, bidOrder);

            askOrder.updateQuantityQNT(Convert.safeSubtract(askOrder.getQuantityQNT(), trade.getQuantityQNT()));
            Account askAccount = Account.getAccount(askOrder.getAccountId());
            askAccount.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeMultiply(trade.getQuantityQNT(), trade.getPriceNQT()));
            askAccount.addToAssetBalanceQNT(assetId, -trade.getQuantityQNT());

            bidOrder.updateQuantityQNT(Convert.safeSubtract(bidOrder.getQuantityQNT(), trade.getQuantityQNT()));
            Account bidAccount = Account.getAccount(bidOrder.getAccountId());
            bidAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, trade.getQuantityQNT());
            bidAccount.addToBalanceNQT(-Convert.safeMultiply(trade.getQuantityQNT(), trade.getPriceNQT()));
            bidAccount.addToUnconfirmedBalanceNQT(Convert.safeMultiply(trade.getQuantityQNT(), (bidOrder.getPriceNQT() - trade.getPriceNQT())));

        }

    }

    static void init() {
        Ask.init();
        Bid.init();
    }


    private final long id;
    private final long accountId;
    private final long assetId;
    private final long priceNQT;
    private final int creationHeight;

    private long quantityQNT;

    private Order(Transaction transaction, Attachment.ColoredCoinsOrderPlacement attachment) {
        this.id = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.assetId = attachment.getAssetId();
        this.quantityQNT = attachment.getQuantityQNT();
        this.priceNQT = attachment.getPriceNQT();
        this.creationHeight = transaction.getHeight();
    }

    private Order(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.assetId = rs.getLong("asset_id");
        this.priceNQT = rs.getLong("price");
        this.quantityQNT = rs.getLong("quantity");
        this.creationHeight = rs.getInt("creation_height");
    }

    private void save(Connection con, String table) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, account_id, asset_id, "
                + "price, quantity, creation_height, height, latest) KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getAssetId());
            pstmt.setLong(++i, this.getPriceNQT());
            pstmt.setLong(++i, this.getQuantityQNT());
            pstmt.setInt(++i, this.getHeight());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getPriceNQT() {
        return priceNQT;
    }

    public final long getQuantityQNT() {
        return quantityQNT;
    }

    public int getHeight() {
        return creationHeight;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " id: " + Convert.toUnsignedLong(id) + " account: " + Convert.toUnsignedLong(accountId)
                + " asset: " + Convert.toUnsignedLong(assetId) + " price: " + priceNQT + " quantity: " + quantityQNT + " height: " + creationHeight;
    }

    private void setQuantityQNT(long quantityQNT) {
        this.quantityQNT = quantityQNT;
    }

    /*
    private int compareTo(Order o) {
        if (height < o.height) {
            return -1;
        } else if (height > o.height) {
            return 1;
        } else {
            if (id < o.id) {
                return -1;
            } else if (id > o.id) {
                return 1;
            } else {
                return 0;
            }
        }

    }
    */

    public static final class Ask extends Order {

        private static final DbKey.LongKeyFactory<Ask> askOrderDbKeyFactory = new DbKey.LongKeyFactory<Ask>("id") {

            @Override
            public DbKey newKey(Ask ask) {
                return ask.dbKey;
            }

        };

        private static final VersionedEntityDbTable<Ask> askOrderTable = new VersionedEntityDbTable<Ask>("ask_order", askOrderDbKeyFactory) {
            @Override
            protected Ask load(Connection con, ResultSet rs) throws SQLException {
                return new Ask(rs);
            }

            @Override
            protected void save(Connection con, Ask ask) throws SQLException {
                ask.save(con, table);
            }

            @Override
            protected String defaultSort() {
                return " ORDER BY creation_height DESC ";
            }

        };

        public static int getCount() {
            return askOrderTable.getCount();
        }

        public static Ask getAskOrder(long orderId) {
            return askOrderTable.get(askOrderDbKeyFactory.newKey(orderId));
        }

        public static DbIterator<Ask> getAll(int from, int to) {
            return askOrderTable.getAll(from, to);
        }

        public static DbIterator<Ask> getAskOrdersByAccount(long accountId, int from, int to) {
            return askOrderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
        }

        public static DbIterator<Ask> getAskOrdersByAsset(long assetId, int from, int to) {
            return askOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
        }

        public static DbIterator<Ask> getAskOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
            DbClause dbClause = new DbClause(" account_id = ? AND asset_id = ? ") {
                @Override
                public int set(PreparedStatement pstmt, int index) throws SQLException {
                    pstmt.setLong(index++, accountId);
                    pstmt.setLong(index++, assetId);
                    return index;
                }
            };
            return askOrderTable.getManyBy(dbClause, from, to);
        }

        public static DbIterator<Ask> getSortedOrders(long assetId, int from, int to) {
            return askOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to,
                    " ORDER BY price ASC, creation_height ASC, id ASC ");
        }

        private static Ask getNextOrder(long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ask_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price ASC, creation_height ASC, id ASC LIMIT 1")) {
                pstmt.setLong(1, assetId);
                try (DbIterator<Ask> askOrders = askOrderTable.getManyBy(con, pstmt, true)) {
                    return askOrders.hasNext() ? askOrders.next() : null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        static void addOrder(Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment) {
            Ask order = new Ask(transaction, attachment);
            askOrderTable.insert(order);
            matchOrders(attachment.getAssetId());
        }

        static void removeOrder(long orderId) {
            askOrderTable.delete(getAskOrder(orderId));
        }

        static void init() {}


        private final DbKey dbKey;

        private Ask(Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment) {
            super(transaction, attachment);
            this.dbKey = askOrderDbKeyFactory.newKey(super.id);
        }

        private Ask(ResultSet rs) throws SQLException {
            super(rs);
            this.dbKey = askOrderDbKeyFactory.newKey(super.id);
        }

        private void save(Connection con, String table) throws SQLException {
            super.save(con, table);
        }

        private void updateQuantityQNT(long quantityQNT) {
            super.setQuantityQNT(quantityQNT);
            if (quantityQNT > 0) {
                askOrderTable.insert(this);
            } else if (quantityQNT == 0) {
                askOrderTable.delete(this);
            } else {
                throw new IllegalArgumentException("Negative quantity: " + quantityQNT
                        + " for order: " + Convert.toUnsignedLong(getId()));
            }
        }

        /*
        @Override
        public int compareTo(Ask o) {
            if (this.getPriceNQT() < o.getPriceNQT()) {
                return -1;
            } else if (this.getPriceNQT() > o.getPriceNQT()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }
        */

    }

    public static final class Bid extends Order {

        private static final DbKey.LongKeyFactory<Bid> bidOrderDbKeyFactory = new DbKey.LongKeyFactory<Bid>("id") {

            @Override
            public DbKey newKey(Bid bid) {
                return bid.dbKey;
            }

        };

        private static final VersionedEntityDbTable<Bid> bidOrderTable = new VersionedEntityDbTable<Bid>("bid_order", bidOrderDbKeyFactory) {

            @Override
            protected Bid load(Connection con, ResultSet rs) throws SQLException {
                return new Bid(rs);
            }

            @Override
            protected void save(Connection con, Bid bid) throws SQLException {
                bid.save(con, table);
            }

            @Override
            protected String defaultSort() {
                return " ORDER BY creation_height DESC ";
            }

        };

        public static int getCount() {
            return bidOrderTable.getCount();
        }

        public static Bid getBidOrder(long orderId) {
            return bidOrderTable.get(bidOrderDbKeyFactory.newKey(orderId));
        }

        public static DbIterator<Bid> getAll(int from, int to) {
            return bidOrderTable.getAll(from, to);
        }

        public static DbIterator<Bid> getBidOrdersByAccount(long accountId, int from, int to) {
            return bidOrderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
        }

        public static DbIterator<Bid> getBidOrdersByAsset(long assetId, int from, int to) {
            return bidOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
        }

        public static DbIterator<Bid> getBidOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
            DbClause dbClause = new DbClause(" account_id = ? AND asset_id = ? ") {
                @Override
                public int set(PreparedStatement pstmt, int index) throws SQLException {
                    pstmt.setLong(index++, accountId);
                    pstmt.setLong(index++, assetId);
                    return index;
                }
            };
            return bidOrderTable.getManyBy(dbClause, from, to);
        }

        public static DbIterator<Bid> getSortedOrders(long assetId, int from, int to) {
            return bidOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to,
                    " ORDER BY price DESC, creation_height ASC, id ASC ");
        }

        private static Bid getNextOrder(long assetId) {
            try (Connection con = Db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM bid_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price DESC, creation_height ASC, id ASC LIMIT 1")) {
                pstmt.setLong(1, assetId);
                try (DbIterator<Bid> bidOrders = bidOrderTable.getManyBy(con, pstmt, true)) {
                    return bidOrders.hasNext() ? bidOrders.next() : null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        static void addOrder(Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment) {
            Bid order = new Bid(transaction, attachment);
            bidOrderTable.insert(order);
            matchOrders(attachment.getAssetId());
        }

        static void removeOrder(long orderId) {
            bidOrderTable.delete(getBidOrder(orderId));
        }

        static void init() {}


        private final DbKey dbKey;

        private Bid(Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment) {
            super(transaction, attachment);
            this.dbKey = bidOrderDbKeyFactory.newKey(super.id);
        }

        private Bid(ResultSet rs) throws SQLException {
            super(rs);
            this.dbKey = bidOrderDbKeyFactory.newKey(super.id);
        }

        private void save(Connection con, String table) throws SQLException {
            super.save(con, table);
        }

        private void updateQuantityQNT(long quantityQNT) {
            super.setQuantityQNT(quantityQNT);
            if (quantityQNT > 0) {
                bidOrderTable.insert(this);
            } else if (quantityQNT == 0) {
                bidOrderTable.delete(this);
            } else {
                throw new IllegalArgumentException("Negative quantity: " + quantityQNT
                        + " for order: " + Convert.toUnsignedLong(getId()));
            }
        }

        /*
        @Override
        public int compareTo(Bid o) {
            if (this.getPriceNQT() > o.getPriceNQT()) {
                return -1;
            } else if (this.getPriceNQT() < o.getPriceNQT()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }
        */
    }
}
