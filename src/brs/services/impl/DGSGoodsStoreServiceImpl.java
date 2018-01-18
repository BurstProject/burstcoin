package brs.services.impl;

import brs.DigitalGoodsStore.Goods;
import brs.db.BurstIterator;
import brs.db.VersionedEntityTable;
import brs.db.store.DigitalGoodsStoreStore;
import brs.services.DGSGoodsStoreService;

public class DGSGoodsStoreServiceImpl implements DGSGoodsStoreService {

  private final DigitalGoodsStoreStore digitalGoodsStoreStore;
  private VersionedEntityTable<Goods> goodsTable;

  public DGSGoodsStoreServiceImpl(DigitalGoodsStoreStore digitalGoodsStoreStore) {
    this.digitalGoodsStoreStore = digitalGoodsStoreStore;
    this.goodsTable = digitalGoodsStoreStore.getGoodsTable();
  }

  @Override
  public BurstIterator<Goods> getAllGoods(int from, int to) {
    return goodsTable.getAll(from, to);
  }

  @Override
  public BurstIterator<Goods> getGoodsInStock(int from, int to) {
    return digitalGoodsStoreStore.getGoodsInStock(from, to);
  }

  @Override
  public BurstIterator<Goods> getSellerGoods(final long sellerId, final boolean inStockOnly, int from, int to) {
    return digitalGoodsStoreStore.getSellerGoods(sellerId, inStockOnly, from, to);
  }
}
