package brs;

import brs.crypto.EncryptedData;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.VersionedValuesTable;
//import brs.db.sql.*;
import brs.services.AccountService;
import brs.services.DGSGoodsStoreService;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Listeners;

import java.util.ArrayList;
import java.util.List;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;

public final class DigitalGoodsStore {

  public enum Event {
    GOODS_LISTED, GOODS_DELISTED, GOODS_PRICE_CHANGE, GOODS_QUANTITY_CHANGE,
    PURCHASE, DELIVERY, REFUND, FEEDBACK
  }

  static class DevNullListener implements Listener<Block> {

    private final AccountService accountService;
    private final DGSGoodsStoreService goodsService;

    DevNullListener(AccountService accountService, DGSGoodsStoreService goodsService) {
      this.accountService = accountService;
      this.goodsService = goodsService;
    }

    @Override
    public void notify(Block block) {
      try (BurstIterator<Purchase> purchases = getExpiredPendingPurchases(block.getTimestamp())) {
        while (purchases.hasNext()) {
          Purchase purchase = purchases.next();
          Account buyer = accountService.getAccount(purchase.getBuyerId());
          buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
          goodsService.getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
          purchase.setPending(false);
        }
      }
    }
  }


  private static final Listeners<Goods,Event> goodsListeners = new Listeners<>();

  private static final Listeners<Purchase,Event> purchaseListeners = new Listeners<>();

  public static boolean addGoodsListener(Listener<Goods> listener, Event eventType) {
    return goodsListeners.addListener(listener, eventType);
  }

  public static boolean removeGoodsListener(Listener<Goods> listener, Event eventType) {
    return goodsListeners.removeListener(listener, eventType);
  }

  public static boolean addPurchaseListener(Listener<Purchase> listener, Event eventType) {
    return purchaseListeners.addListener(listener, eventType);
  }

  public static boolean removePurchaseListener(Listener<Purchase> listener, Event eventType) {
    return purchaseListeners.removeListener(listener, eventType);
  }

  static void init() {
    Goods.init();
    Purchase.init();
  }

  public static class Goods {

    private static final BurstKey.LongKeyFactory<Goods> goodsDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getGoodsDbKeyFactory();
    }

    private static final VersionedEntityTable<Goods> goodsTable() {
      return Burst.getStores().getDigitalGoodsStoreStore().getGoodsTable();
    }

    static void init() {}


    private final long id;
    public final BurstKey dbKey;
    private final long sellerId;
    private final String name;
    private final String description;
    private final String tags;
    private final int timestamp;
    private int quantity;
    private long priceNQT;
    private boolean delisted;

    protected Goods(long id, BurstKey dbKey, long sellerId, String name, String description, String tags, int timestamp,
                    int quantity, long priceNQT, boolean delisted) {
      this.id = id;
      this.dbKey = dbKey;
      this.sellerId = sellerId;
      this.name = name;
      this.description = description;
      this.tags = tags;
      this.timestamp = timestamp;
      this.quantity = quantity;
      this.priceNQT = priceNQT;
      this.delisted = delisted;
    }

    private Goods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
      this.id = transaction.getId();
      this.dbKey = goodsDbKeyFactory().newKey(this.id);
      this.sellerId = transaction.getSenderId();
      this.name = attachment.getName();
      this.description = attachment.getDescription();
      this.tags = attachment.getTags();
      this.quantity = attachment.getQuantity();
      this.priceNQT = attachment.getPriceNQT();
      this.delisted = false;
      this.timestamp = transaction.getTimestamp();
    }



    public long getId() {
      return id;
    }

    public long getSellerId() {
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

    public int getTimestamp() {
      return timestamp;
    }

    public int getQuantity() {
      return quantity;
    }

    private void changeQuantity(int deltaQuantity) {
      quantity += deltaQuantity;
      if (quantity < 0) {
        quantity = 0;
      } else if (quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
        quantity = Constants.MAX_DGS_LISTING_QUANTITY;
      }
      goodsTable().insert(this);
    }

    public long getPriceNQT() {
      return priceNQT;
    }

    private void changePrice(long priceNQT) {
      this.priceNQT = priceNQT;
      goodsTable().insert(this);
    }

    public boolean isDelisted() {
      return delisted;
    }

    private void setDelisted(boolean delisted) {
      this.delisted = delisted;
      goodsTable().insert(this);
    }

    /*
      @Override
      public int compareTo(Goods other) {
      if (!name.equals(other.name)) {
      return name.compareTo(other.name);
      }
      if (!description.equals(other.description)) {
      return description.compareTo(other.description);
      }
      return Long.compare(id, other.id);
      }
    */

  }

  public static  class Purchase {

    private static final BurstKey.LongKeyFactory<Purchase> purchaseDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getPurchaseDbKeyFactory();
    }

    private static final VersionedEntityTable<Purchase> purchaseTable() {
      return Burst.getStores().getDigitalGoodsStoreStore().getPurchaseTable();
    }


    private static final BurstKey.LongKeyFactory<Purchase> feedbackDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getFeedbackDbKeyFactory();
    }

    private static final VersionedValuesTable<Purchase, EncryptedData> feedbackTable() {
      return Burst.getStores().getDigitalGoodsStoreStore().getFeedbackTable();
    }

    private static final BurstKey.LongKeyFactory<Purchase> publicFeedbackDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getPublicFeedbackDbKeyFactory();
    }

    private static final VersionedValuesTable<Purchase, String> publicFeedbackTable() {
      return Burst.getStores().getDigitalGoodsStoreStore().getPublicFeedbackTable();
    }

    static void init() {}

    private final long id;
    public final BurstKey dbKey;
    private final long buyerId;
    private final long goodsId;
    private final long sellerId;
    private final int quantity;
    private final long priceNQT;
    private final int deadline;
    private final EncryptedData note;
    private final int timestamp;
    private boolean isPending;
    private EncryptedData encryptedGoods;
    private boolean goodsIsText;
    private EncryptedData refundNote;
    private boolean hasFeedbackNotes;
    private List<EncryptedData> feedbackNotes;
    private boolean hasPublicFeedbacks;
    private List<String> publicFeedbacks;
    private long discountNQT;
    private long refundNQT;

    private Purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, long sellerId) {
      this.id = transaction.getId();
      this.dbKey = purchaseDbKeyFactory().newKey(this.id);
      this.buyerId = transaction.getSenderId();
      this.goodsId = attachment.getGoodsId();
      this.sellerId = sellerId;
      this.quantity = attachment.getQuantity();
      this.priceNQT = attachment.getPriceNQT();
      this.deadline = attachment.getDeliveryDeadlineTimestamp();
      this.note = transaction.getEncryptedMessage() == null ? null : transaction.getEncryptedMessage().getEncryptedData();
      this.timestamp = transaction.getTimestamp();
      this.isPending = true;
    }

    protected Purchase(long id, BurstKey dbKey, long buyerId, long goodsId, long sellerId, int quantity,
                       long priceNQT, int deadline, EncryptedData note, int timestamp, boolean isPending,
                       EncryptedData encryptedGoods,EncryptedData refundNote,
                       boolean hasFeedbackNotes, boolean hasPublicFeedbacks,
                       long discountNQT, long refundNQT) {
      this.id = id;
      this.dbKey = dbKey;
      this.buyerId = buyerId;
      this.goodsId = goodsId;
      this.sellerId = sellerId;
      this.quantity = quantity;
      this.priceNQT = priceNQT;
      this.deadline = deadline;
      this.note = note;
      this.timestamp = timestamp;
      this.isPending = isPending;
      this.encryptedGoods = encryptedGoods;
      this.refundNote = refundNote;
      this.hasFeedbackNotes = hasFeedbackNotes;
      this.hasPublicFeedbacks = hasPublicFeedbacks;
      this.discountNQT = discountNQT;
      this.refundNQT = refundNQT;
    }

    public long getId() {
      return id;
    }

    public long getBuyerId() {
      return buyerId;
    }

    public long getGoodsId() {
      return goodsId;
    }

    public long getSellerId() { return sellerId; }

    public int getQuantity() {
      return quantity;
    }

    public long getPriceNQT() {
      return priceNQT;
    }

    public int getDeliveryDeadlineTimestamp() {
      return deadline;
    }

    public EncryptedData getNote() {
      return note;
    }

    public boolean isPending() {
      return isPending;
    }

    private void setPending(boolean isPending) {
      this.isPending = isPending;
      purchaseTable().insert(this);
    }

    public int getTimestamp() {
      return timestamp;
    }

    public String getName() {
      return getGoods(goodsId).getName();
    }

    public EncryptedData getEncryptedGoods() {
      return encryptedGoods;
    }

    public boolean goodsIsText() {
      return goodsIsText;
    }

    private void setEncryptedGoods(EncryptedData encryptedGoods, boolean goodsIsText) {
      this.encryptedGoods = encryptedGoods;
      this.goodsIsText = goodsIsText;
      purchaseTable().insert(this);
    }

    public EncryptedData getRefundNote() {
      return refundNote;
    }

    private void setRefundNote(EncryptedData refundNote) {
      this.refundNote = refundNote;
      purchaseTable().insert(this);
    }

    public List<EncryptedData> getFeedbackNotes() {
      if (!hasFeedbackNotes) {
        return null;
      }
      feedbackNotes = feedbackTable().get(feedbackDbKeyFactory().newKey(this));
      return feedbackNotes;
    }

    private void addFeedbackNote(EncryptedData feedbackNote) {
      if (feedbackNotes == null) {
        feedbackNotes = new ArrayList<>();
      }
      feedbackNotes.add(feedbackNote);
      this.hasFeedbackNotes = true;
      purchaseTable().insert(this);
      feedbackTable().insert(this, feedbackNotes);
    }

    public List<String> getPublicFeedback() {
      if (!hasPublicFeedbacks) {
        return null;
      }
      publicFeedbacks =  publicFeedbackTable().get(publicFeedbackDbKeyFactory().newKey(this));
      return publicFeedbacks;
    }

    private void addPublicFeedback(String publicFeedback) {
      if (publicFeedbacks == null) {
        publicFeedbacks = new ArrayList<>();
      }
      publicFeedbacks.add(publicFeedback);
      this.hasPublicFeedbacks = true;
      purchaseTable().insert(this);
      publicFeedbackTable().insert(this, publicFeedbacks);
    }

    public long getDiscountNQT() {
      return discountNQT;
    }

    public void setDiscountNQT(long discountNQT) {
      this.discountNQT = discountNQT;
      purchaseTable().insert(this);
    }

    public long getRefundNQT() {
      return refundNQT;
    }

    public void setRefundNQT(long refundNQT) {
      this.refundNQT = refundNQT;
      purchaseTable().insert(this);
    }

  }

  public static Goods getGoods(long goodsId) {
    return Goods.goodsTable().get(Goods.goodsDbKeyFactory().newKey(goodsId));
  }

  public static Purchase getPurchase(long purchaseId) {
    return Purchase.purchaseTable().get(Purchase.purchaseDbKeyFactory().newKey(purchaseId));
  }

  static Purchase getPendingPurchase(long purchaseId) {
    Purchase purchase = getPurchase(purchaseId);
    return purchase == null || ! purchase.isPending() ? null : purchase;
  }

  private static BurstIterator<Purchase> getExpiredPendingPurchases(final int timestamp) {
    return Burst.getStores().getDigitalGoodsStoreStore().getExpiredPendingPurchases(timestamp);
  }

  private static void addPurchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment, long sellerId) {
    Purchase purchase = new Purchase(transaction, attachment, sellerId);
    Purchase.purchaseTable().insert(purchase);
    purchaseListeners.notify(purchase, Event.PURCHASE);
  }

  static void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
    Goods goods = new Goods(transaction, attachment);
    Goods.goodsTable().insert(goods);
    goodsListeners.notify(goods, Event.GOODS_LISTED);
  }

  static void delistGoods(long goodsId) {
    Goods goods = Goods.goodsTable().get(Goods.goodsDbKeyFactory().newKey(goodsId));
    if (! goods.isDelisted()) {
      goods.setDelisted(true);
      goodsListeners.notify(goods, Event.GOODS_DELISTED);
    } else {
      throw new IllegalStateException("Goods already delisted");
    }
  }

  static void changePrice(long goodsId, long priceNQT) {
    Goods goods = Goods.goodsTable().get(Goods.goodsDbKeyFactory().newKey(goodsId));
    if (! goods.isDelisted()) {
      goods.changePrice(priceNQT);
      goodsListeners.notify(goods, Event.GOODS_PRICE_CHANGE);
    } else {
      throw new IllegalStateException("Can't change price of delisted goods");
    }
  }

  static void changeQuantity(long goodsId, int deltaQuantity) {
    Goods goods = Goods.goodsTable().get(Goods.goodsDbKeyFactory().newKey(goodsId));
    if (! goods.isDelisted()) {
      goods.changeQuantity(deltaQuantity);
      goodsListeners.notify(goods, Event.GOODS_QUANTITY_CHANGE);
    } else {
      throw new IllegalStateException("Can't change quantity of delisted goods");
    }
  }

  static void purchase(Transaction transaction,  Attachment.DigitalGoodsPurchase attachment) {
    Goods goods = Goods.goodsTable().get(Goods.goodsDbKeyFactory().newKey(attachment.getGoodsId()));
    if (! goods.isDelisted() && attachment.getQuantity() <= goods.getQuantity() && attachment.getPriceNQT() == goods.getPriceNQT()
        && attachment.getDeliveryDeadlineTimestamp() > Burst.getBlockchain().getLastBlock().getTimestamp()) {
      goods.changeQuantity(-attachment.getQuantity());
      addPurchase(transaction, attachment, goods.getSellerId());
    } else {
      Account buyer = Account.getAccount(transaction.getSenderId());
      buyer.addToUnconfirmedBalanceNQT(Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
      // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
    }
  }

  static void deliver(Transaction transaction, Attachment.DigitalGoodsDelivery attachment) {
    Purchase purchase = getPendingPurchase(attachment.getPurchaseId());
    if ( purchase == null ) {
      throw new RuntimeException("cant find purchase with id " + attachment.getPurchaseId());
    }
    purchase.setPending(false);
    long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
    Account buyer = Account.getAccount(purchase.getBuyerId());
    buyer.addToBalanceNQT(Convert.safeSubtract(attachment.getDiscountNQT(), totalWithoutDiscount));
    buyer.addToUnconfirmedBalanceNQT(attachment.getDiscountNQT());
    Account seller = Account.getAccount(transaction.getSenderId());
    seller.addToBalanceAndUnconfirmedBalanceNQT(Convert.safeSubtract(totalWithoutDiscount, attachment.getDiscountNQT()));
    purchase.setEncryptedGoods(attachment.getGoods(), attachment.goodsIsText());
    purchase.setDiscountNQT(attachment.getDiscountNQT());
    purchaseListeners.notify(purchase, Event.DELIVERY);
  }

  static void refund(long sellerId, long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage) {
    Purchase purchase = Purchase.purchaseTable().get(Purchase.purchaseDbKeyFactory().newKey(purchaseId));
    Account seller = Account.getAccount(sellerId);
    seller.addToBalanceNQT(-refundNQT);
    Account buyer = Account.getAccount(purchase.getBuyerId());
    buyer.addToBalanceAndUnconfirmedBalanceNQT(refundNQT);
    if (encryptedMessage != null) {
      purchase.setRefundNote(encryptedMessage.getEncryptedData());
    }
    purchase.setRefundNQT(refundNQT);
    purchaseListeners.notify(purchase, Event.REFUND);
  }

  static void feedback(long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message) {
    Purchase purchase = Purchase.purchaseTable().get(Purchase.purchaseDbKeyFactory().newKey(purchaseId));
    if (encryptedMessage != null) {
      purchase.addFeedbackNote(encryptedMessage.getEncryptedData());
    }
    if (message != null) {
      purchase.addPublicFeedback(Convert.toString(message.getMessage()));
    }
    purchaseListeners.notify(purchase, Event.FEEDBACK);
  }


}
