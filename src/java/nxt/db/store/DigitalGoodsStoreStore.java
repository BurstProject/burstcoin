package nxt.db.store;

import nxt.DigitalGoodsStore;
import nxt.crypto.EncryptedData;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;
import nxt.db.VersionedValuesTable;


public interface DigitalGoodsStoreStore {

    NxtKey.LongKeyFactory<DigitalGoodsStore.Purchase> getFeedbackDbKeyFactory();

    NxtKey.LongKeyFactory<DigitalGoodsStore.Purchase> getPurchaseDbKeyFactory();

    VersionedEntityTable<DigitalGoodsStore.Purchase> getPurchaseTable();

    VersionedValuesTable<DigitalGoodsStore.Purchase, EncryptedData> getFeedbackTable();

    NxtKey.LongKeyFactory<DigitalGoodsStore.Purchase> getPublicFeedbackDbKeyFactory();

    VersionedValuesTable<DigitalGoodsStore.Purchase, String> getPublicFeedbackTable();

    NxtKey.LongKeyFactory<DigitalGoodsStore.Goods> getGoodsDbKeyFactory();

    VersionedEntityTable<DigitalGoodsStore.Goods> getGoodsTable();

    NxtIterator<DigitalGoodsStore.Goods> getGoodsInStock(int from, int to);

    NxtIterator<DigitalGoodsStore.Goods> getSellerGoods(long sellerId, boolean inStockOnly, int from, int to);

    NxtIterator<DigitalGoodsStore.Purchase> getAllPurchases(int from, int to);

    NxtIterator<DigitalGoodsStore.Purchase> getSellerPurchases(long sellerId, int from, int to);

    NxtIterator<DigitalGoodsStore.Purchase> getBuyerPurchases(long buyerId, int from, int to);

    NxtIterator<DigitalGoodsStore.Purchase> getSellerBuyerPurchases(long sellerId, long buyerId, int from, int to);

    NxtIterator<DigitalGoodsStore.Purchase> getPendingSellerPurchases(long sellerId, int from, int to);

    NxtIterator<DigitalGoodsStore.Purchase> getExpiredPendingPurchases(int timestamp);
}
