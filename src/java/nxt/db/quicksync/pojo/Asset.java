package nxt.db.quicksync.pojo;


public class Asset {

  private long db_Id;
  private long id;
  private long account_Id;
  private String name;
  private String description;
  private long quantity;
  private long decimals;
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


  public long getAccount_Id() {
    return account_Id;
  }

  public void setAccount_Id(long account_Id) {
    this.account_Id = account_Id;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }


  public long getQuantity() {
    return quantity;
  }

  public void setQuantity(long quantity) {
    this.quantity = quantity;
  }


  public long getDecimals() {
    return decimals;
  }

  public void setDecimals(long decimals) {
    this.decimals = decimals;
  }


  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }

}
