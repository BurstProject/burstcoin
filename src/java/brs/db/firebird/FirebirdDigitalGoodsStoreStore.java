package brs.db.firebird;

import brs.DigitalGoodsStore;
import brs.Nxt;
import brs.db.NxtIterator;
import brs.db.VersionedEntityTable;
import brs.db.VersionedValuesTable;
import brs.db.sql.DbClause;
import brs.db.sql.SqlDigitalGoodsStoreStore;
import brs.db.sql.VersionedEntitySqlTable;
import brs.db.sql.VersionedValuesSqlTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class FirebirdDigitalGoodsStoreStore extends SqlDigitalGoodsStoreStore {
    private final VersionedValuesTable<DigitalGoodsStore.Purchase, String> publicFeedbackTable =
            new VersionedValuesSqlTable<DigitalGoodsStore.Purchase, String>("purchase_public_feedback", publicFeedbackDbKeyFactory) {

                @Override
                protected String load(Connection con, ResultSet rs) throws SQLException {
                    return rs.getString("public_feedback");
                }

                @Override
                protected void save(Connection con, DigitalGoodsStore.Purchase purchase, String publicFeedback) throws SQLException {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE OR INSERT INTO purchase_public_feedback (id, public_feedback, "
                            + "height, latest) VALUES (?, ?, ?, TRUE) MATCHING (id, height)")) {
                        int i = 0;
                        pstmt.setLong(++i, purchase.getId());
                        pstmt.setString(++i, publicFeedback);
                        pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                        pstmt.executeUpdate();
                    }
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
            return " ORDER BY \"timestamp\" DESC, id ASC ";
        }

    };

    @Override
    public VersionedValuesTable<DigitalGoodsStore.Purchase, String> getPublicFeedbackTable() {
        return publicFeedbackTable;
    }

    @Override
    protected void saveGoods(Connection con, DigitalGoodsStore.Goods goods) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("UPDATE OR INSERT INTO goods (id, seller_id, name, "
                + "description, tags, \"timestamp\", quantity, price, delisted, height, latest) "
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) MATCHING (id, height)")) {
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

    @Override
    protected void savePurchase(Connection con, DigitalGoodsStore.Purchase purchase) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("UPDATE OR INSERT INTO purchase (id, buyer_id, goods_id, seller_id, "
                + "quantity, price, deadline, note, nonce, \"timestamp\", pending, goods, goods_nonce, refund_note, "
                + "refund_nonce, has_feedback_notes, has_public_feedbacks, discount, refund, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) MATCHING (id, height)")) {
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
    public VersionedEntityTable<DigitalGoodsStore.Purchase> getPurchaseTable() {
        return purchaseTable;
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
        return getGoodsTable().getManyBy(dbClause, from, to, " ORDER BY name ASC, \"timestamp\" DESC, id ASC ");
    }
}
