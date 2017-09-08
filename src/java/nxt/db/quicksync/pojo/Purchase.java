package nxt.db.quicksync.pojo;


public class Purchase {

  private long db_Id;
  private long id;
  private long buyer_Id;
  private long goods_Id;
  private long seller_Id;
  private long quantity;
  private long price;
  private long deadline;
  private String note;
  private String nonce;
  private long timestamp;
  private long pending;
  private String goods;
  private String goods_Nonce;
  private String refund_Note;
  private String refund_Nonce;
  private long has_Feedback_Notes;
  private long has_Public_Feedbacks;
  private long discount;
  private long refund;
  private long height;
  private long latest;


  public long getDb_Id() {
    return db_Id;
  }

  public void setDb_Id(long db_Id) {
    this.db_Id = db_Id;
  }


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public long getBuyer_Id() {
    return buyer_Id;
  }

  public void setBuyer_Id(long buyer_Id) {
    this.buyer_Id = buyer_Id;
  }


  public long getGoods_Id() {
    return goods_Id;
  }

  public void setGoods_Id(long goods_Id) {
    this.goods_Id = goods_Id;
  }


  public long getSeller_Id() {
    return seller_Id;
  }

  public void setSeller_Id(long seller_Id) {
    this.seller_Id = seller_Id;
  }


  public long getQuantity() {
    return quantity;
  }

  public void setQuantity(long quantity) {
    this.quantity = quantity;
  }


  public long getPrice() {
    return price;
  }

  public void setPrice(long price) {
    this.price = price;
  }


  public long getDeadline() {
    return deadline;
  }

  public void setDeadline(long deadline) {
    this.deadline = deadline;
  }


  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }


  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }


  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  public long getPending() {
    return pending;
  }

  public void setPending(long pending) {
    this.pending = pending;
  }


  public String getGoods() {
    return goods;
  }

  public void setGoods(String goods) {
    this.goods = goods;
  }


  public String getGoods_Nonce() {
    return goods_Nonce;
  }

  public void setGoods_Nonce(String goods_Nonce) {
    this.goods_Nonce = goods_Nonce;
  }


  public String getRefund_Note() {
    return refund_Note;
  }

  public void setRefund_Note(String refund_Note) {
    this.refund_Note = refund_Note;
  }


  public String getRefund_Nonce() {
    return refund_Nonce;
  }

  public void setRefund_Nonce(String refund_Nonce) {
    this.refund_Nonce = refund_Nonce;
  }


  public long getHas_Feedback_Notes() {
    return has_Feedback_Notes;
  }

  public void setHas_Feedback_Notes(long has_Feedback_Notes) {
    this.has_Feedback_Notes = has_Feedback_Notes;
  }


  public long getHas_Public_Feedbacks() {
    return has_Public_Feedbacks;
  }

  public void setHas_Public_Feedbacks(long has_Public_Feedbacks) {
    this.has_Public_Feedbacks = has_Public_Feedbacks;
  }


  public long getDiscount() {
    return discount;
  }

  public void setDiscount(long discount) {
    this.discount = discount;
  }


  public long getRefund() {
    return refund;
  }

  public void setRefund(long refund) {
    this.refund = refund;
  }


  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }


  public long getLatest() {
    return latest;
  }

  public void setLatest(long latest) {
    this.latest = latest;
  }

}
