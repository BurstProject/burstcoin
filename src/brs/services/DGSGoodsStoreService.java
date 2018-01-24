package brs.services;

import brs.DigitalGoodsStore.Goods;
import brs.DigitalGoodsStore.Purchase;
import brs.db.BurstIterator;

public interface DGSGoodsStoreService {

  Goods getGoods(long goodsId);

  BurstIterator<Goods> getAllGoods(int from, int to);

  BurstIterator<Goods> getGoodsInStock(int from, int to);

  BurstIterator<Goods> getSellerGoods(long sellerId, boolean inStockOnly, int from, int to);

  BurstIterator<Purchase> getAllPurchases(int from, int to);

  BurstIterator<Purchase> getSellerPurchases(long sellerId, int from, int to);

  BurstIterator<Purchase> getBuyerPurchases(long buyerId, int from, int to);

  BurstIterator<Purchase> getSellerBuyerPurchases(long sellerId, long buyerId, int from, int to);

  BurstIterator<Purchase> getPendingSellerPurchases(long sellerId, int from, int to);
}
