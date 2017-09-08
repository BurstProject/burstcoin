package nxt.db.quicksync.pojo;


public class Bid_Order {

  private long db_Id;
  private long id;
  private long account_Id;
  private long asset_Id;
  private long price;
  private long quantity;
  private long creation_Height;
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


  public long getAccount_Id() {
    return account_Id;
  }

  public void setAccount_Id(long account_Id) {
    this.account_Id = account_Id;
  }


  public long getAsset_Id() {
    return asset_Id;
  }

  public void setAsset_Id(long asset_Id) {
    this.asset_Id = asset_Id;
  }


  public long getPrice() {
    return price;
  }

  public void setPrice(long price) {
    this.price = price;
  }


  public long getQuantity() {
    return quantity;
  }

  public void setQuantity(long quantity) {
    this.quantity = quantity;
  }


  public long getCreation_Height() {
    return creation_Height;
  }

  public void setCreation_Height(long creation_Height) {
    this.creation_Height = creation_Height;
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
