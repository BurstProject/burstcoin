package nxt.db.quicksync.pojo;


public class Trade {

  private long db_Id;
  private long asset_Id;
  private long block_Id;
  private long ask_Order_Id;
  private long bid_Order_Id;
  private long ask_Order_Height;
  private long bid_Order_Height;
  private long seller_Id;
  private long buyer_Id;
  private long quantity;
  private long price;
  private long timestamp;
  private long height;


  public long getDb_Id() {
    return db_Id;
  }

  public void setDb_Id(long db_Id) {
    this.db_Id = db_Id;
  }


  public long getAsset_Id() {
    return asset_Id;
  }

  public void setAsset_Id(long asset_Id) {
    this.asset_Id = asset_Id;
  }


  public long getBlock_Id() {
    return block_Id;
  }

  public void setBlock_Id(long block_Id) {
    this.block_Id = block_Id;
  }


  public long getAsk_Order_Id() {
    return ask_Order_Id;
  }

  public void setAsk_Order_Id(long ask_Order_Id) {
    this.ask_Order_Id = ask_Order_Id;
  }


  public long getBid_Order_Id() {
    return bid_Order_Id;
  }

  public void setBid_Order_Id(long bid_Order_Id) {
    this.bid_Order_Id = bid_Order_Id;
  }


  public long getAsk_Order_Height() {
    return ask_Order_Height;
  }

  public void setAsk_Order_Height(long ask_Order_Height) {
    this.ask_Order_Height = ask_Order_Height;
  }


  public long getBid_Order_Height() {
    return bid_Order_Height;
  }

  public void setBid_Order_Height(long bid_Order_Height) {
    this.bid_Order_Height = bid_Order_Height;
  }


  public long getSeller_Id() {
    return seller_Id;
  }

  public void setSeller_Id(long seller_Id) {
    this.seller_Id = seller_Id;
  }


  public long getBuyer_Id() {
    return buyer_Id;
  }

  public void setBuyer_Id(long buyer_Id) {
    this.buyer_Id = buyer_Id;
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


  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }

}
