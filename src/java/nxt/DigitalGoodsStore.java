package nxt;

import nxt.crypto.XoredData;
import nxt.util.Convert;
import nxt.util.Listener;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DigitalGoodsStore {

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Map.Entry<Long, Purchase> pendingPurchaseEntry : pendingPurchases.entrySet()) {
                    Purchase purchase = pendingPurchaseEntry.getValue();
                    if (block.getTimestamp() > purchase.getDeliveryDeadline()) {
                        Account buyer = Account.getAccount(purchase.getBuyerId());
                        buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                        getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
                        pendingPurchases.remove(pendingPurchaseEntry.getKey());
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

        // reverse any pending purchase expiration that was caused by the block that got popped off
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                Block previousBlock = Nxt.getBlockchain().getLastBlock();
                for (Map.Entry<Long, Purchase> purchaseEntry : purchases.entrySet()) {
                    Purchase purchase = purchaseEntry.getValue();
                    if (block.getTimestamp() > purchase.getDeliveryDeadline()
                            && previousBlock.getTimestamp() <= purchase.getDeliveryDeadline()) {
                        Account buyer = Account.getAccount(purchase.getBuyerId());
                        buyer.addToUnconfirmedBalanceNQT(- Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
                        getGoods(purchase.getGoodsId()).changeQuantity(- purchase.getQuantity());
                        pendingPurchases.put(purchaseEntry.getKey(), purchase);
                    }
                }
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);
    }

    public static final class Goods {
        private final Long id;
        private final Long sellerId;
        private final String name;
        private final String description;
        private final String tags;
        private volatile int quantity;
        private volatile long priceNQT;
        private volatile boolean delisted;

        private Goods(Long id, Long sellerId, String name, String description, String tags, int quantity, long priceNQT) {
            this.id = id;
            this.sellerId = sellerId;
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
        }

        public Long getId() {
            return id;
        }

        public Long getSellerId() {
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

        public int getQuantity() {
            return quantity;
        }

        private void changeQuantity(int deltaQuantity) {
            quantity += deltaQuantity;
            if (quantity < 0) {
                quantity = 0;
            } else if (quantity > Constants.MAX_DIGITAL_GOODS_QUANTITY) {
                quantity = Constants.MAX_DIGITAL_GOODS_QUANTITY;
            }
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        private void changePrice(long priceNQT) {
            this.priceNQT = priceNQT;
        }

        public boolean isDelisted() {
            return delisted;
        }

        private void setDelisted(boolean delisted) {
            this.delisted = delisted;
        }

    }

    public static final class Purchase {
        private final Long id;
        private final Long buyerId;
        private final Long goodsId;
        private final Long sellerId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadline;
        private final XoredData note;

        private Purchase(Long id, Long buyerId, Long goodsId, Long sellerId, int quantity, long priceNQT, int deliveryDeadline, XoredData note) {
            this.id = id;
            this.buyerId = buyerId;
            this.goodsId = goodsId;
            this.sellerId = sellerId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadline = deliveryDeadline;
            this.note = note;
        }

        public Long getId() {
            return id;
        }

        public Long getBuyerId() {
            return buyerId;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public Long getSellerId() { return sellerId; }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public int getDeliveryDeadline() {
            return deliveryDeadline;
        }

        public XoredData getNote() {
            return note;
        }
    }

    private static final ConcurrentMap<Long, Goods> goods = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Purchase> purchases = new ConcurrentHashMap<>();
    private static final Collection<Goods> allGoods = Collections.unmodifiableCollection(goods.values());
    private static final Collection<Purchase> allPurchases = Collections.unmodifiableCollection(purchases.values());
    private static final ConcurrentMap<Long, Purchase> pendingPurchases = new ConcurrentHashMap<>();

    public static Collection<Goods> getAllGoods() {
        return allGoods;
    }

    public static Collection<Purchase> getAllPurchases() {
        return allPurchases;
    }

    public static Goods getGoods(Long goodsId) {
        return goods.get(goodsId);
    }

    public static Purchase getPurchase(Long purchaseId) {
        return purchases.get(purchaseId);
    }

    public static Purchase getPendingPurchase(Long purchaseId) {
        return pendingPurchases.get(purchaseId);
    }

    private static void addPurchase(Long purchaseId, Long buyerId, Long goodsId, Long sellerId, int quantity, long priceNQT,
                                   int deliveryDeadline, XoredData note) {
        Purchase purchase = new Purchase(purchaseId, buyerId, goodsId, sellerId, quantity, priceNQT, deliveryDeadline, note);
        purchases.put(purchaseId, purchase);
        pendingPurchases.put(purchaseId, purchase);
    }

    static void clear() {
        goods.clear();
        purchases.clear();
    }

    static void listGoods(Long goodsId, Long sellerId, String name, String description, String tags,
                                 int quantity, long priceNQT) {
        goods.put(goodsId, new Goods(goodsId, sellerId, name, description, tags, quantity, priceNQT));
    }

    static void undoListGoods(Long goodsId) {
        goods.remove(goodsId);
    }

    static void delistGoods(Long goodsId) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.setDelisted(true);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    static void undoDelistGoods(Long goodsId) {
        Goods goods = getGoods(goodsId);
        if (goods.isDelisted()) {
            goods.setDelisted(false);
        } else {
            throw new IllegalStateException("Goods were not delisted");
        }
    }

    static void changePrice(Long goodsId, long priceNQT) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.changePrice(priceNQT);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    static void changeQuantity(Long goodsId, int deltaQuantity) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    static void purchase(Long purchaseId, Long buyerId, Long goodsId, int quantity, long priceNQT,
                                int deliveryDeadline, XoredData note) {
        Goods goods = getGoods(goodsId);
        if (! goods.isDelisted() && quantity <= goods.getQuantity() && priceNQT == goods.getPriceNQT()
                && deliveryDeadline > Nxt.getBlockchain().getLastBlock().getHeight()) {
            goods.changeQuantity(-quantity);
            addPurchase(purchaseId, buyerId, goodsId, goods.getSellerId(), quantity, priceNQT, deliveryDeadline, note);
        } else {
            Account buyer = Account.getAccount(buyerId);
            buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(quantity, priceNQT));
            // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
        }
    }

    static void undoPurchase(Long purchaseId, Long buyerId, int quantity, long priceNQT) {
        Purchase purchase = purchases.remove(purchaseId);
        if (purchase != null) {
            pendingPurchases.remove(purchaseId);
            getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
        } else {
            Account buyer = Account.getAccount(buyerId);
            buyer.addToUnconfirmedBalanceNQT(- Convert.safeMultiply(quantity, priceNQT));
        }
    }

    static void deliver(Long sellerId, Long purchaseId, long discountNQT) {
        Purchase purchase = pendingPurchases.remove(purchaseId);
        if (purchase != null) {
            long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToBalanceNQT(Convert.safeSubtract(discountNQT, totalWithoutDiscount));
            buyer.addToUnconfirmedBalanceNQT(discountNQT);
            Account seller = Account.getAccount(sellerId);
            seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, discountNQT));
        }
    }

    static void undoDeliver(Long sellerId, Long purchaseId, long discountNQT) {
        Purchase purchase = purchases.get(purchaseId);
        if (purchase != null) {
            pendingPurchases.put(purchaseId, purchase);
            long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, discountNQT));
            buyer.addToUnconfirmedBalanceNQT(- discountNQT);
            Account seller = Account.getAccount(sellerId);
            seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(discountNQT, totalWithoutDiscount));
        }
    }

    static void refund(Long sellerId, Long purchaseId, long refundNQT) {
        Purchase purchase = getPurchase(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(-refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(refundNQT);
    }

    static void undoRefund(Long sellerId, Long purchaseId, long refundNQT) {
        Purchase purchase = getPurchase(purchaseId);
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceNQT(refundNQT);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceNQT(-refundNQT);
    }

}
