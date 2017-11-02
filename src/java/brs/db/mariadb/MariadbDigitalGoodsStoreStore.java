package brs.db.mariadb;

import brs.DigitalGoodsStore;
import brs.Nxt;
import brs.db.VersionedValuesTable;
import brs.db.sql.SqlDigitalGoodsStoreStore;
import brs.db.sql.VersionedValuesSqlTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class MariadbDigitalGoodsStoreStore extends SqlDigitalGoodsStoreStore {
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

    @Override
    public VersionedValuesTable<DigitalGoodsStore.Purchase, String> getPublicFeedbackTable() {
        return publicFeedbackTable;
    }

    @Override
    protected void saveGoods(Connection con, DigitalGoodsStore.Goods goods) throws SQLException {
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

    @Override
    protected void savePurchase(Connection con, DigitalGoodsStore.Purchase purchase) throws SQLException {
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
}
