package nxt.db.quicksync.pojo;


public class Account_Asset {

  private long db_Id;
  private long account_Id;
  private long asset_Id;
  private long quantity;
  private long unconfirmed_Quantity;
  private long height;
  private long latest;


  public long getDb_Id() {
    return db_Id;
  }

  public void setDb_Id(long db_Id) {
    this.db_Id = db_Id;
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


  public long getQuantity() {
    return quantity;
  }

  public void setQuantity(long quantity) {
    this.quantity = quantity;
  }


  public long getUnconfirmed_Quantity() {
    return unconfirmed_Quantity;
  }

  public void setUnconfirmed_Quantity(long unconfirmed_Quantity) {
    this.unconfirmed_Quantity = unconfirmed_Quantity;
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
