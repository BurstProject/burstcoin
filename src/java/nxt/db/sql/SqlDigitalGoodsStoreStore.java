package nxt.db.sql;

import nxt.DigitalGoodsStore;
import nxt.Nxt;
import nxt.crypto.EncryptedData;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;
import nxt.db.VersionedValuesTable;
import nxt.db.store.DigitalGoodsStoreStore;

import java.sql.*;

public abstract class SqlDigitalGoodsStoreStore implements DigitalGoodsStoreStore {

    private static final DbKey.LongKeyFactory<DigitalGoodsStore.Purchase> feedbackDbKeyFactory = new DbKey.LongKeyFactory<DigitalGoodsStore.Purchase>("id") {

        @Override
        public NxtKey newKey(DigitalGoodsStore.Purchase purchase) {
            return purchase.dbKey;
        }

    };
    private final NxtKey.LongKeyFactory<DigitalGoodsStore.Purchase> purchaseDbKeyFactory = new DbKey.LongKeyFactory<DigitalGoodsStore.Purchase>("id") {

        @Override
        public NxtKey newKey(DigitalGoodsStore.Purchase purchase) {
            return purchase.dbKey;
        }

    };
    private final VersionedEntityTable<DigitalGoodsStore.Purchase> purchaseTable = new VersionedEntitySqlTable<DigitalGoodsStore.Purchase>("purchase", purchaseDbKeyFactory) {

        @Override
        protected DigitalGoodsStore.Purchase load(Connection con, ResultSet rs) throws SQLException {
            return new SQLPurchase(rs);
        }

        @Override
        protected void save(Connection con, DigitalGoodsStore.Purchase purchase) throws SQLException {
            savePurchase(con, purchase);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY timestamp DESC, id ASC ";
        }

    };
    @Deprecated
    private final VersionedValuesTable<DigitalGoodsStore.Purchase, EncryptedData> feedbackTable =
            new VersionedValuesSqlTable<DigitalGoodsStore.Purchase, EncryptedData>("purchase_feedback", feedbackDbKeyFactory) {

        @Override
        protected EncryptedData load(Connection con, ResultSet rs) throws SQLException {
            byte[] data = rs.getBytes("feedback_data");
            byte[] nonce = rs.getBytes("feedback_nonce");
            return new EncryptedData(data, nonce);
        }

        @Override
        protected void save(Connection con, DigitalGoodsStore.Purchase purchase, EncryptedData encryptedData) throws SQLException {
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

    private final DbKey.LongKeyFactory<DigitalGoodsStore.Purchase> publicFeedbackDbKeyFactory = new DbKey.LongKeyFactory<DigitalGoodsStore.Purchase>("id") {

        @Override
        public NxtKey newKey(DigitalGoodsStore.Purchase purchase) {
            return purchase.dbKey;
        }

    };

    private final VersionedValuesTable<DigitalGoodsStore.Purchase, String> publicFeedbackTable =
            new VersionedValuesSqlTable<DigitalGoodsStore.Purchase, String>("purchase_public_feedback", publicFeedbackDbKeyFactory) {

                @Override
                protected String load(Connection con, ResultSet rs) throws SQLException {
                    return rs.getString("public_feedback");
                }

                @Override
                protected void save(Connection con, DigitalGoodsStore.Purchase purchase, String publicFeedback) throws SQLException {
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
    private final NxtKey.LongKeyFactory<DigitalGoodsStore.Goods> goodsDbKeyFactory = new DbKey.LongKeyFactory<DigitalGoodsStore.Goods>("id") {

        @Override
        public NxtKey newKey(DigitalGoodsStore.Goods goods) {
            return goods.dbKey;
        }

    };
    private final VersionedEntityTable<DigitalGoodsStore.Goods> goodsTable = new VersionedEntitySqlTable<DigitalGoodsStore.Goods>("goods", goodsDbKeyFactory) {

        @Override
        protected DigitalGoodsStore.Goods load(Connection con, ResultSet rs) throws SQLException {
            return new SQLGoods(rs);
        }

        @Override
        protected void save(Connection con, DigitalGoodsStore.Goods goods) throws SQLException {
            saveGoods(con, goods);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY timestamp DESC, id ASC ";
        }

    };

    @Override
    public NxtIterator<DigitalGoodsStore.Purchase> getExpiredPendingPurchases(final int timestamp) {
        DbClause dbClause = new DbClause(" deadline < ? AND pending = TRUE ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, timestamp);
                return index;
            }
        };
        return purchaseTable.getManyBy(dbClause, 0, -1);
    }

    private void setEncryptedData(PreparedStatement pstmt, EncryptedData encryptedData, int i) throws SQLException {
        if (encryptedData == null) {
            pstmt.setNull(i, Types.VARBINARY);
            pstmt.setNull(i + 1, Types.VARBINARY);
        } else {
            pstmt.setBytes(i, encryptedData.getData());
            pstmt.setBytes(i + 1, encryptedData.getNonce());
        }
    }

    private EncryptedData loadEncryptedData(ResultSet rs, String dataColumn, String nonceColumn) throws SQLException {
        byte[] data = rs.getBytes(dataColumn);
        if (data == null) {
            return null;
        }
        return new EncryptedData(data, rs.getBytes(nonceColumn));
    }

    @Override
    public NxtKey.LongKeyFactory<DigitalGoodsStore.Purchase> getFeedbackDbKeyFactory() {
        return feedbackDbKeyFactory;
    }

    @Override
    public NxtKey.LongKeyFactory<DigitalGoodsStore.Purchase> getPurchaseDbKeyFactory() {
        return purchaseDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<DigitalGoodsStore.Purchase> getPurchaseTable() {
        return purchaseTable;
    }

    @Override
    public VersionedValuesTable<DigitalGoodsStore.Purchase, EncryptedData> getFeedbackTable() {
        return feedbackTable;
    }

    @Override
    public DbKey.LongKeyFactory<DigitalGoodsStore.Purchase> getPublicFeedbackDbKeyFactory() {
        return publicFeedbackDbKeyFactory;
    }

    @Override
    public VersionedValuesTable<DigitalGoodsStore.Purchase, String> getPublicFeedbackTable() {
        return publicFeedbackTable;
    }

    @Override
    public NxtKey.LongKeyFactory<DigitalGoodsStore.Goods> getGoodsDbKeyFactory() {
        return goodsDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<DigitalGoodsStore.Goods> getGoodsTable() {
        return goodsTable;
    }

    private void saveGoods(Connection con, DigitalGoodsStore.Goods goods) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO goods (id, seller_id, name, "
                + "description, tags, timestamp, quantity, price, delisted, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, goods.getId());
            pstmt.setLong(++i, goods.getSellerId());
            pstmt.setString(++i, goods.getName());
            pstmt.setString(++i, goods.getDescription());
            pstmt.setString(++i, goods.getTags());
            pstmt.setInt(++i, goods.getTimestamp());
            pstmt.setInt(++i, goods.getQuantity());
            pstmt.setLong(++i, goods.getPriceNQT());
            pstmt.setBoolean(++i, goods.isDelisted());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    private void savePurchase(Connection con, DigitalGoodsStore.Purchase purchase) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO purchase (id, buyer_id, goods_id, seller_id, "
                + "quantity, price, deadline, note, nonce, timestamp, pending, goods, goods_nonce, refund_note, "
                + "refund_nonce, has_feedback_notes, has_public_feedbacks, discount, refund, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, purchase.getId());
            pstmt.setLong(++i, purchase.getBuyerId());
            pstmt.setLong(++i, purchase.getGoodsId());
            pstmt.setLong(++i, purchase.getSellerId());
            pstmt.setInt(++i, purchase.getQuantity());
            pstmt.setLong(++i, purchase.getPriceNQT());
            pstmt.setInt(++i, purchase.getDeliveryDeadlineTimestamp());
            setEncryptedData(pstmt, purchase.getNote(), ++i);
            ++i;
            pstmt.setInt(++i, purchase.getTimestamp());
            pstmt.setBoolean(++i, purchase.isPending());
            setEncryptedData(pstmt, purchase.getEncryptedGoods(), ++i);
            ++i;
            setEncryptedData(pstmt, purchase.getRefundNote(), ++i);
            ++i;
            pstmt.setBoolean(++i, purchase.getFeedbackNotes() != null && purchase.getFeedbackNotes().size() > 0);
            pstmt.setBoolean(++i, purchase.getPublicFeedback() != null && purchase.getPublicFeedback().size() > 0);
            pstmt.setLong(++i, purchase.getDiscountNQT());
            pstmt.setLong(++i, purchase.getRefundNQT());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public NxtIterator<DigitalGoodsStore.Goods> getGoodsInStock(int from, int to) {
        DbClause dbClause = new DbClause(" delisted = FALSE AND quantity > 0 ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                return index;
            }
        };
        return goodsTable.getManyBy(dbClause, from, to);
    }

    @Override
    public NxtIterator<DigitalGoodsStore.Goods> getSellerGoods(final long sellerId, final boolean inStockOnly, int from, int to) {
        DbClause dbClause = new DbClause(" seller_id = ? " + (inStockOnly ? "AND delisted = FALSE AND quantity > 0" : "")) {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, sellerId);
                return index;
            }
        };
        return goodsTable.getManyBy(dbClause, from, to, " ORDER BY name ASC, timestamp DESC, id ASC ");
    }

    @Override
    public NxtIterator<DigitalGoodsStore.Purchase> getAllPurchases(int from, int to) {
        return purchaseTable.getAll(from, to);
    }

    @Override
    public NxtIterator<DigitalGoodsStore.Purchase> getSellerPurchases(long sellerId, int from, int to) {
        return purchaseTable.getManyBy(new DbClause.LongClause("seller_id", sellerId), from, to);
    }

    @Override
    public NxtIterator<DigitalGoodsStore.Purchase> getBuyerPurchases(long buyerId, int from, int to) {
        return purchaseTable.getManyBy(new DbClause.LongClause("buyer_id", buyerId), from, to);
    }

    @Override
    public NxtIterator<DigitalGoodsStore.Purchase> getSellerBuyerPurchases(final long sellerId, final long buyerId, int from, int to) {
        DbClause dbClause = new DbClause(" seller_id = ? AND buyer_id = ? ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, sellerId);
                pstmt.setLong(index++, buyerId);
                return index;
            }
        };
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    @Override
    public NxtIterator<DigitalGoodsStore.Purchase> getPendingSellerPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause(" seller_id = ? AND pending = TRUE ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, sellerId);
                return index;
            }
        };
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    public DigitalGoodsStore.Purchase getPendingPurchase(long purchaseId) {
        DigitalGoodsStore.Purchase purchase =
                purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
        return purchase == null || !purchase.isPending() ? null : purchase;
    }



    private class SQLGoods extends DigitalGoodsStore.Goods {
        private SQLGoods(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("id"),
                    goodsDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getLong("seller_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("tags"),
                    rs.getInt("timestamp"),
                    rs.getInt("quantity"),
                    rs.getLong("price"),
                    rs.getBoolean("delisted")
            );
        }
    }




    private class SQLPurchase extends DigitalGoodsStore.Purchase {

        private SQLPurchase(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("id"),
                    purchaseDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getLong("buyer_id"),
                    rs.getLong("goods_id"),
                    rs.getLong("seller_id"),
                    rs.getInt("quantity"),
                    rs.getLong("price"),
                    rs.getInt("deadline"),
                    loadEncryptedData(rs, "note", "nonce"),
                    rs.getInt("timestamp"),
                    rs.getBoolean("pending"),
                    loadEncryptedData(rs, "goods", "goods_nonce"),
                    loadEncryptedData(rs, "refund_note", "refund_nonce"),
                    rs.getBoolean("has_feedback_notes"),
                    rs.getBoolean("has_public_feedbacks"),
                    rs.getLong("discount"),
                    rs.getLong("refund")
            );
        }
    }

}
