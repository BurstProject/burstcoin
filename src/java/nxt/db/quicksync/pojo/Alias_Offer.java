package nxt.db.quicksync.pojo;


public class Alias_Offer {

  private long db_Id;
  private long id;
  private long price;
  private Long buyer_Id;
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


  public long getPrice() {
    return price;
  }

  public void setPrice(long price) {
    this.price = price;
  }


  public long getBuyer_Id() {
    return buyer_Id;
  }

  public void setBuyer_Id(long buyer_Id) {
    this.buyer_Id = buyer_Id;
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
