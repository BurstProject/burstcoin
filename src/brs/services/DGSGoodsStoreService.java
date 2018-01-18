package brs.services;

import brs.DigitalGoodsStore.Goods;
import brs.db.BurstIterator;

public interface DGSGoodsStoreService {

  BurstIterator<Goods> getAllGoods(int from, int to);

  BurstIterator<Goods> getGoodsInStock(int from, int to);

  BurstIterator<Goods> getSellerGoods(long sellerId, boolean inStockOnly, int from, int to);
}
