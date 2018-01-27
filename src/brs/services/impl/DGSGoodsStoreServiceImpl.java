package brs.services.impl;

import brs.DigitalGoodsStore.Goods;
import brs.DigitalGoodsStore.Purchase;
import brs.db.BurstIterator;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedEntityTable;
import brs.db.store.DigitalGoodsStoreStore;
import brs.services.DGSGoodsStoreService;

public class DGSGoodsStoreServiceImpl implements DGSGoodsStoreService {

  private final DigitalGoodsStoreStore digitalGoodsStoreStore;
  private VersionedEntityTable<Goods> goodsTable;
  private final VersionedEntityTable<Purchase> purchaseTable;
  private final LongKeyFactory<Goods> goodsDbKeyFactory;
  private final LongKeyFactory<Purchase> purchaseDbKeyFactory;

  public DGSGoodsStoreServiceImpl(DigitalGoodsStoreStore digitalGoodsStoreStore) {
    this.digitalGoodsStoreStore = digitalGoodsStoreStore;
    this.goodsTable = digitalGoodsStoreStore.getGoodsTable();
    this.purchaseTable = digitalGoodsStoreStore.getPurchaseTable();
    this.goodsDbKeyFactory = digitalGoodsStoreStore.getGoodsDbKeyFactory();
    this.purchaseDbKeyFactory = digitalGoodsStoreStore.getPurchaseDbKeyFactory();
  }

  @Override
  public Goods getGoods(long goodsId) {
    return goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
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

  @Override
  public BurstIterator<Purchase> getAllPurchases(int from, int to) {
    return purchaseTable.getAll(from, to);
  }

  @Override
  public BurstIterator<Purchase> getSellerPurchases(long sellerId, int from, int to) {
    return digitalGoodsStoreStore.getSellerPurchases(sellerId, from, to);
  }

  @Override
  public BurstIterator<Purchase> getBuyerPurchases(long buyerId, int from, int to) {
    return digitalGoodsStoreStore.getBuyerPurchases(buyerId, from, to);
  }

  @Override
  public BurstIterator<Purchase> getSellerBuyerPurchases(final long sellerId, final long buyerId, int from, int to) {
    return digitalGoodsStoreStore.getSellerBuyerPurchases(sellerId, buyerId, from, to);
  }

  @Override
  public BurstIterator<Purchase> getPendingSellerPurchases(final long sellerId, int from, int to) {
    return digitalGoodsStoreStore.getPendingSellerPurchases(sellerId, from, to);
  }

  @Override
  public Purchase getPurchase(long purchaseId) {
    return purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
  }

}
