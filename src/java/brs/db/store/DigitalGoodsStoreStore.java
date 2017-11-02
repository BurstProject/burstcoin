package brs.db.store;

import brs.DigitalGoodsStore;
import brs.crypto.EncryptedData;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.VersionedValuesTable;


public interface DigitalGoodsStoreStore {

    BurstKey.LongKeyFactory<DigitalGoodsStore.Purchase> getFeedbackDbKeyFactory();

    BurstKey.LongKeyFactory<DigitalGoodsStore.Purchase> getPurchaseDbKeyFactory();

    VersionedEntityTable<DigitalGoodsStore.Purchase> getPurchaseTable();

    VersionedValuesTable<DigitalGoodsStore.Purchase, EncryptedData> getFeedbackTable();

    BurstKey.LongKeyFactory<DigitalGoodsStore.Purchase> getPublicFeedbackDbKeyFactory();

    VersionedValuesTable<DigitalGoodsStore.Purchase, String> getPublicFeedbackTable();

    BurstKey.LongKeyFactory<DigitalGoodsStore.Goods> getGoodsDbKeyFactory();

    VersionedEntityTable<DigitalGoodsStore.Goods> getGoodsTable();

    BurstIterator<DigitalGoodsStore.Goods> getGoodsInStock(int from, int to);

    BurstIterator<DigitalGoodsStore.Goods> getSellerGoods(long sellerId, boolean inStockOnly, int from, int to);

    BurstIterator<DigitalGoodsStore.Purchase> getAllPurchases(int from, int to);

    BurstIterator<DigitalGoodsStore.Purchase> getSellerPurchases(long sellerId, int from, int to);

    BurstIterator<DigitalGoodsStore.Purchase> getBuyerPurchases(long buyerId, int from, int to);

    BurstIterator<DigitalGoodsStore.Purchase> getSellerBuyerPurchases(long sellerId, long buyerId, int from, int to);

    BurstIterator<DigitalGoodsStore.Purchase> getPendingSellerPurchases(long sellerId, int from, int to);

    BurstIterator<DigitalGoodsStore.Purchase> getExpiredPendingPurchases(int timestamp);
}
