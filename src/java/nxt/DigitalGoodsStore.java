package nxt;

import nxt.crypto.EncryptedData;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.db.VersionedValuesDbTable;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class DigitalGoodsStore {

    public static enum Event {
        GOODS_LISTED, GOODS_DELISTED, GOODS_PRICE_CHANGE, GOODS_QUANTITY_CHANGE,
        PURCHASE, DELIVERY, REFUND, FEEDBACK
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                try (DbIterator<Purchase> purchases = getExpiredPendingPurchases(block.getTimestamp())) {
                    while (purchases.hasNext()) {
                        Purchase purchase = purchases.next();
                        Account buyer = Account.getAccount(purchase.getBuyerId());
                        buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                        getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
                        purchase.setPending(false);
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static final Listeners<Goods,Event> goodsListeners = new Listeners<>();

    private static final Listeners<Purchase,Event> purchaseListeners = new Listeners<>();

    public static boolean addGoodsListener(Listener<Goods> listener, Event eventType) {
        return goodsListeners.addListener(listener, eventType);
    }

    public static boolean removeGoodsListener(Listener<Goods> listener, Event eventType) {
        return goodsListeners.removeListener(listener, eventType);
    }

    public static boolean addPurchaseListener(Listener<Purchase> listener, Event eventType) {
        return purchaseListeners.addListener(listener, eventType);
    }

    public static boolean removePurchaseListener(Listener<Purchase> listener, Event eventType) {
        return purchaseListeners.removeListener(listener, eventType);
    }

    static void init() {
        Goods.init();
        Purchase.init();
    }

    public static final class Goods {

        private static final DbKey.LongKeyFactory<Goods> goodsDbKeyFactory = new DbKey.LongKeyFactory<Goods>("id") {

            @Override
            public DbKey newKey(Goods goods) {
                return goods.dbKey;
            }

        };

        private static final VersionedEntityDbTable<Goods> goodsTable = new VersionedEntityDbTable<Goods>("goods", goodsDbKeyFactory) {

            @Override
            protected Goods load(Connection con, ResultSet rs) throws SQLException {
                return new Goods(rs);
            }

            @Override
            protected void save(Connection con, Goods goods) throws SQLException {
                goods.save(con);
            }

            @Override
            protected String defaultSort() {
                return " ORDER BY timestamp DESC, id ASC ";
            }

        };

        static void init() {}


        private final long id;
        private final DbKey dbKey;
        private final long sellerId;
        private final String name;
        private final String description;
        private final String tags;
        private final int timestamp;
        private int quantity;
        private long priceNQT;
        private boolean delisted;

        private Goods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
            this.id = transaction.getId();
            this.dbKey = goodsDbKeyFactory.newKey(this.id);
            this.sellerId = transaction.getSenderId();
            this.name = attachment.getName();
            this.description = attachment.getDescription();
            this.tags = attachment.getTags();
            this.quantity = attachment.getQuantity();
            this.priceNQT = attachment.getPriceNQT();
            this.delisted = false;
            this.timestamp = transaction.getTimestamp();
        }

        private Goods(ResultSet rs) throws SQLException {
            this.id = rs.getLong("id");
            this.dbKey = goodsDbKeyFactory.newKey(this.id);
            this.sellerId = rs.getLong("seller_id");
            this.name = rs.getString("name");
            this.description = rs.getString("description");
            this.tags = rs.getString("tags");
            this.quantity = rs.getInt("quantity");
            this.priceNQT = rs.getLong("price");
            this.delisted = rs.getBoolean("delisted");
            this.timestamp = rs.getInt("timestamp");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO goods (id, seller_id, name, "
                    + "description, tags, timestamp, quantity, price, delisted, height, latest) KEY (id, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.getId());
                pstmt.setLong(++i, this.getSellerId());
                pstmt.setString(++i, this.getName());
                pstmt.setString(++i, this.getDescription());
                pstmt.setString(++i, this.getTags());
                pstmt.setInt(++i, this.getTimestamp());
                pstmt.setInt(++i, this.getQuantity());
                pstmt.setLong(++i, this.getPriceNQT());
                pstmt.setBoolean(++i, this.isDelisted());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getId() {
            return id;
        }

        public long getSellerId() {
            return sellerId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTags() {
            return tags;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public int getQuantity() {
            return quantity;
        }

        private void changeQuantity(int deltaQuantity) {
            quantity += deltaQuantity;
            if (quantity < 0) {
                quantity = 0;
            } else if (quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
                quantity = Constants.MAX_DGS_LISTING_QUANTITY;
            }
            goodsTable.insert(this);
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        private void changePrice(long priceNQT) {
            this.priceNQT = priceNQT;
            goodsTable.insert(this);
        }

        public boolean isDelisted() {
            return delisted;
        }

        private void setDelisted(boolean delisted) {
            this.delisted = delisted;
            goodsTable.insert(this);
        }

        /*
        @Override
        public int compareTo(Goods other) {
            if (!name.equals(other.name)) {
                return name.compareTo(other.name);
            }
            if (!description.equals(other.description)) {
                return description.compareTo(other.description);
            }
            return Long.compare(id, other.id);
        }
        */

    }

    public static final class Purchase {

        private static final DbKey.LongKeyFactory<Purchase> purchaseDbKeyFactory = new DbKey.LongKeyFactory<Purchase>("id") {

            @Override
            public DbKey newKey(Purchase purchase) {
                return purchase.dbKey;
            }

        };

        private static final VersionedEntityDbTable<Purchase> purchaseTable = new VersionedEntityDbTable<Purchase>("purchase", purchaseDbKeyFactory) {

            @Override
            protected Purchase load(Connection con, ResultSet rs) throws SQLException {
                return new Purchase(rs);
            }

            @Override
            protected void save(Connection con, Purchase purchase) throws SQLException {
                purchase.save(con);
            }

            @Override
            protected String defaultSort() {
                return " ORDER BY timestamp DESC, id ASC ";
            }

        };

        private static final DbKey.LongKeyFactory<Purchase> feedbackDbKeyFactory = new DbKey.LongKeyFactory<Purchase>("id") {

            @Override
            public DbKey newKey(Purchase purchase) {
                return purchase.dbKey;
            }

        };

        private static final VersionedValuesDbTable<Purchase, EncryptedData> feedbackTable = new VersionedValuesDbTable<Purchase, EncryptedData>("purchase_feedback", feedbackDbKeyFactory) {

            @Override
            protected EncryptedData load(Connection con, ResultSet rs) throws SQLException {
                byte[] data = rs.getBytes("feedback_data");
                byte[] nonce = rs.getBytes("feedback_nonce");
                return new EncryptedData(data, nonce);
            }

            @Override
            protected void save(Connection con, Purchase purchase, EncryptedData encryptedData) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_feedback (id, feedback_data, feedback_nonce, "
                        + "height, latest) VALUES (?, ?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, purchase.getId());
                    setEncryptedData(pstmt, encryptedData, ++i);
                    ++i;
                    pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                    pstmt.executeUpdate();
                }
            }

        };

        private static final DbKey.LongKeyFactory<Purchase> publicFeedbackDbKeyFactory = new DbKey.LongKeyFactory<Purchase>("id") {

            @Override
            public DbKey newKey(Purchase purchase) {
                return purchase.dbKey;
            }

        };

        private static final VersionedValuesDbTable<Purchase, String> publicFeedbackTable = new VersionedValuesDbTable<Purchase, String>("purchase_public_feedback", publicFeedbackDbKeyFactory) {

            @Override
            protected String load(Connection con, ResultSet rs) throws SQLException {
                return rs.getString("public_feedback");
            }

            @Override
            protected void save(Connection con, Purchase purchase, String publicFeedback) throws SQLException {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase_public_feedback (id, public_feedback, "
                        + "height, latest) VALUES (?, ?, ?, TRUE)")) {
                    int i = 0;
                    pstmt.setLong(++i, purchase.getId());
                    pstmt.setString(++i, publicFeedback);
                    pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                    pstmt.executeUpdate();
                }
            }

        };

        static void init() {}


        private final long id;
        private final DbKey dbKey;
        private final long buyerId;
        private final long goodsId;
        private final long sellerId;
        private final int quantity;
        private final long priceNQT;
        private final int deadline;
        private final EncryptedData note;
        private final int timestamp;
        private boolean isPending;
        private EncryptedData encryptedGoods;
		private boolean goodsIsText;
        private EncryptedData refundNote;
        private boolean hasFeedbackNotes;
        private List<EncryptedData> feedbackNotes;
        private boolean hasPublicFeedbacks;
        private List<String> publicFeedbacks;
        private long discountNQT;
        private long refundNQT;

        private Purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, long sellerId) {
            this.id = transaction.getId();
            this.dbKey = purchaseDbKeyFactory.newKey(this.id);
            this.buyerId = transaction.getSenderId();
            this.goodsId = attachment.getGoodsId();
            this.sellerId = sellerId;
            this.quantity = attachment.getQuantity();
            this.priceNQT = attachment.getPriceNQT();
            this.deadline = attachment.getDeliveryDeadlineTimestamp();
            this.note = transaction.getEncryptedMessage() == null ? null : transaction.getEncryptedMessage().getEncryptedData();
            this.timestamp = transaction.getTimestamp();
            this.isPending = true;
        }

        private Purchase(ResultSet rs) throws SQLException {
            this.id = rs.getLong("id");
            this.dbKey = purchaseDbKeyFactory.newKey(this.id);
            this.buyerId = rs.getLong("buyer_id");
            this.goodsId = rs.getLong("goods_id");
            this.sellerId = rs.getLong("seller_id");
            this.quantity = rs.getInt("quantity");
            this.priceNQT = rs.getLong("price");
            this.deadline = rs.getInt("deadline");
            this.note = loadEncryptedData(rs, "note", "nonce");
            this.timestamp = rs.getInt("timestamp");
            this.isPending = rs.getBoolean("pending");
            this.encryptedGoods = loadEncryptedData(rs, "goods", "goods_nonce");
            this.refundNote = loadEncryptedData(rs, "refund_note", "refund_nonce");
            this.hasFeedbackNotes = rs.getBoolean("has_feedback_notes");
            this.hasPublicFeedbacks = rs.getBoolean("has_public_feedbacks");
            this.discountNQT = rs.getLong("discount");
            this.refundNQT = rs.getLong("refund");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO purchase (id, buyer_id, goods_id, seller_id, "
                    + "quantity, price, deadline, note, nonce, timestamp, pending, goods, goods_nonce, refund_note, "
                    + "refund_nonce, has_feedback_notes, has_public_feedbacks, discount, refund, height, latest) KEY (id, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.getId());
                pstmt.setLong(++i, this.getBuyerId());
                pstmt.setLong(++i, this.getGoodsId());
                pstmt.setLong(++i, this.getSellerId());
                pstmt.setInt(++i, this.getQuantity());
                pstmt.setLong(++i, this.getPriceNQT());
                pstmt.setInt(++i, this.getDeliveryDeadlineTimestamp());
                setEncryptedData(pstmt, this.getNote(), ++i);
                ++i;
                pstmt.setInt(++i, this.getTimestamp());
                pstmt.setBoolean(++i, this.isPending());
                setEncryptedData(pstmt, this.getEncryptedGoods(), ++i);
                ++i;
                setEncryptedData(pstmt, this.getRefundNote(), ++i);
                ++i;
                pstmt.setBoolean(++i, this.getFeedbackNotes() != null && this.getFeedbackNotes().size() > 0);
                pstmt.setBoolean(++i, this.getPublicFeedback() != null && this.getPublicFeedback().size() > 0);
                pstmt.setLong(++i, this.getDiscountNQT());
                pstmt.setLong(++i, this.getRefundNQT());
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getId() {
            return id;
        }

        public long getBuyerId() {
            return buyerId;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public long getSellerId() { return sellerId; }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public int getDeliveryDeadlineTimestamp() {
            return deadline;
        }

        public EncryptedData getNote() {
            return note;
        }

        public boolean isPending() {
            return isPending;
        }

        private void setPending(boolean isPending) {
            this.isPending = isPending;
            purchaseTable.insert(this);
        }

        public int getTimestamp() {
            return timestamp;
        }

        public String getName() {
            return getGoods(goodsId).getName();
        }

        public EncryptedData getEncryptedGoods() {
            return encryptedGoods;
        }

        public boolean goodsIsText() {
            return goodsIsText;
        }

        private void setEncryptedGoods(EncryptedData encryptedGoods, boolean goodsIsText) {
            this.encryptedGoods = encryptedGoods;
            this.goodsIsText = goodsIsText;
            purchaseTable.insert(this);
        }

        public EncryptedData getRefundNote() {
            return refundNote;
        }

        private void setRefundNote(EncryptedData refundNote) {
            this.refundNote = refundNote;
            purchaseTable.insert(this);
        }

        public List<EncryptedData> getFeedbackNotes() {
            if (!hasFeedbackNotes) {
                return null;
            }
            feedbackNotes = feedbackTable.get(feedbackDbKeyFactory.newKey(this));
            return feedbackNotes;
        }

        private void addFeedbackNote(EncryptedData feedbackNote) {
            if (feedbackNotes == null) {
                feedbackNotes = new ArrayList<>();
            }
            feedbackNotes.add(feedbackNote);
            this.hasFeedbackNotes = true;
            purchaseTable.insert(this);
            feedbackTable.insert(this, feedbackNotes);
		}

        public List<String> getPublicFeedback() {
            if (!hasPublicFeedbacks) {
                return null;
            }
            publicFeedbacks = publicFeedbackTable.get(publicFeedbackDbKeyFactory.newKey(this));
            return publicFeedbacks;
        }

        private void addPublicFeedback(String publicFeedback) {
            if (publicFeedbacks == null) {
                publicFeedbacks = new ArrayList<>();
            }
            publicFeedbacks.add(publicFeedback);
            this.hasPublicFeedbacks = true;
            purchaseTable.insert(this);
            publicFeedbackTable.insert(this, publicFeedbacks);
        }

        public long getDiscountNQT() {
            return discountNQT;
        }

        public void setDiscountNQT(long discountNQT) {
            this.discountNQT = discountNQT;
            purchaseTable.insert(this);
        }

        public long getRefundNQT() {
            return refundNQT;
        }

        public void setRefundNQT(long refundNQT) {
            this.refundNQT = refundNQT;
            purchaseTable.insert(this);
        }

        /*
        @Override
        public int compareTo(Purchase other) {
            if (this.timestamp < other.timestamp) {
                return 1;
            }
            if (this.timestamp > other.timestamp) {
                return -1;
            }
            return Long.compare(this.id, other.id);
        }
        */

    }

    public static Goods getGoods(long goodsId) {
        return Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
    }

    public static DbIterator<Goods> getAllGoods(int from, int to) {
        return Goods.goodsTable.getAll(from, to);
    }

    public static DbIterator<Goods> getGoodsInStock(int from, int to) {
        DbClause dbClause = new DbClause(" delisted = FALSE AND quantity > 0 ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                return index;
            }
        };
        return Goods.goodsTable.getManyBy(dbClause, from, to);
    }

    public static DbIterator<Goods> getSellerGoods(final long sellerId, final boolean inStockOnly, int from, int to) {
        DbClause dbClause = new DbClause(" seller_id = ? " + (inStockOnly ? "AND delisted = FALSE AND quantity > 0" : "")) {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, sellerId);
                return index;
            }
        };
        return Goods.goodsTable.getManyBy(dbClause, from, to, " ORDER BY name ASC, timestamp DESC, id ASC ");
    }

    public static DbIterator<Purchase> getAllPurchases(int from, int to) {
        return Purchase.purchaseTable.getAll(from, to);
    }

    public static DbIterator<Purchase> getSellerPurchases(long sellerId, int from, int to) {
        return Purchase.purchaseTable.getManyBy(new DbClause.LongClause("seller_id", sellerId), from, to);
    }

    public static DbIterator<Purchase> getBuyerPurchases(long buyerId, int from, int to) {
        return Purchase.purchaseTable.getManyBy(new DbClause.LongClause("buyer_id", buyerId), from, to);
    }

    public static DbIterator<Purchase> getSellerBuyerPurchases(final long sellerId, final long buyerId, int from, int to) {
        DbClause dbClause = new DbClause(" seller_id = ? AND buyer_id = ? ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, sellerId);
                pstmt.setLong(index++, buyerId);
                return index;
            }
        };
        return Purchase.purchaseTable.getManyBy(dbClause, from, to);
    }

    public static Purchase getPurchase(long purchaseId) {
        return Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
    }

    public static DbIterator<Purchase> getPendingSellerPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause(" seller_id = ? AND pending = TRUE ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, sellerId);
                return index;
            }
        };
        return Purchase.purchaseTable.getManyBy(dbClause, from, to);
    }

    static Purchase getPendingPurchase(long purchaseId) {
        Purchase purchase = getPurchase(purchaseId);
        return purchase == null || ! purchase.isPending() ? null : purchase;
    }

    private static DbIterator<Purchase> getExpiredPendingPurchases(final int timestamp) {
        DbClause dbClause = new DbClause(" deadline < ? AND pending = TRUE ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, timestamp);
                return index;
            }
        };
        return Purchase.purchaseTable.getManyBy(dbClause, 0, -1);
	}

    private static void addPurchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment, long sellerId) {
        Purchase purchase = new Purchase(transaction, attachment, sellerId);
        Purchase.purchaseTable.insert(purchase);
        purchaseListeners.notify(purchase, Event.PURCHASE);
    }

    static void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
        Goods goods = new Goods(transaction, attachment);
        Goods.goodsTable.insert(goods);
        goodsListeners.notify(goods, Event.GOODS_LISTED);
    }

    static void delistGoods(long goodsId) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.setDelisted(true);
            goodsListeners.notify(goods, Event.GOODS_DELISTED);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    static void changePrice(long goodsId, long priceNQT) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.changePrice(priceNQT);
            goodsListeners.notify(goods, Event.GOODS_PRICE_CHANGE);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    static void changeQuantity(long goodsId, int deltaQuantity) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
            goodsListeners.notify(goods, Event.GOODS_QUANTITY_CHANGE);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    static void purchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(attachment.getGoodsId()));
        if (! goods.isDelisted() && attachment.getQuantity() <= goods.getQuantity() && attachment.getPriceNQT() == goods.getPriceNQT()
                && attachment.getDeliveryDeadlineTimestamp() > Nxt.getBlockchain().getLastBlock().getTimestamp()) {
            goods.changeQuantity(-attachment.getQuantity());
            addPurchase(transaction, attachment, goods.getSellerId());
        } else {
            Account buyer = Account.getAccount(transaction.getSenderId());
            buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
            // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
        }
    }

    static void deliver(Transaction transaction, Attachment.DigitalGoodsDelivery attachment) {
        Purchase purchase = getPendingPurchase(attachment.getPurchaseId());
        purchase.setPending(false);
        long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceNQT(Convert.safeSubtract(attachment.getDiscountNQT(), totalWithoutDiscount));
        buyer.addToUnconfirmedBalanceNQT(attachment.getDiscountNQT());
        Account seller = Account.getAccount(transaction.getSenderId());
        seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, attachment.getDiscountNQT()));
        purchase.setEncryptedGoods(attachment.getGoods(), attachment.goodsIsText());
        purchase.setDiscountNQT(attachment.getDiscountNQT());
        purchaseListeners.notify(purchase, Event.DELIVERY);
    }

    static void refund(long sellerId, long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage) {
        Purchase purchase = Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(-refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(refundNQT);
        if (encryptedMessage != null) {
            purchase.setRefundNote(encryptedMessage.getEncryptedData());
        }
        purchase.setRefundNQT(refundNQT);
        purchaseListeners.notify(purchase, Event.REFUND);
    }

    static void feedback(long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message) {
        Purchase purchase = Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
        if (encryptedMessage != null) {
            purchase.addFeedbackNote(encryptedMessage.getEncryptedData());
        }
        if (message != null) {
            purchase.addPublicFeedback(Convert.toString(message.getMessage()));
        }
        purchaseListeners.notify(purchase, Event.FEEDBACK);
    }

    private static EncryptedData loadEncryptedData(ResultSet rs, String dataColumn, String nonceColumn) throws SQLException {
        byte[] data = rs.getBytes(dataColumn);
        if (data == null) {
            return null;
        }
        return new EncryptedData(data, rs.getBytes(nonceColumn));
    }

    private static void setEncryptedData(PreparedStatement pstmt, EncryptedData encryptedData, int i) throws SQLException {
        if (encryptedData == null) {
            pstmt.setNull(i, Types.VARBINARY);
            pstmt.setNull(i + 1, Types.VARBINARY);
        } else {
            pstmt.setBytes(i, encryptedData.getData());
            pstmt.setBytes(i + 1, encryptedData.getNonce());
        }
    }

}
