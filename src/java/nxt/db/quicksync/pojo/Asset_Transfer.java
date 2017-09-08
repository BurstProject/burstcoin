package nxt.db.quicksync.pojo;


public class Asset_Transfer {

  private long db_Id;
  private long id;
  private long asset_Id;
  private long sender_Id;
  private long recipient_Id;
  private long quantity;
  private long timestamp;
  private long height;


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


  public long getAsset_Id() {
    return asset_Id;
  }

  public void setAsset_Id(long asset_Id) {
    this.asset_Id = asset_Id;
  }


  public long getSender_Id() {
    return sender_Id;
  }

  public void setSender_Id(long sender_Id) {
    this.sender_Id = sender_Id;
  }


  public long getRecipient_Id() {
    return recipient_Id;
  }

  public void setRecipient_Id(long recipient_Id) {
    this.recipient_Id = recipient_Id;
  }


  public long getQuantity() {
    return quantity;
  }

  public void setQuantity(long quantity) {
    this.quantity = quantity;
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
