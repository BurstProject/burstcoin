package nxt.db.quicksync.pojo;


public class Alias {

  private long db_Id;
  private long id;
  private long account_Id;
  private String alias_Name;
  private String alias_Name_Lower;
  private String alias_Uri;
  private long timestamp;
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


  public String getAlias_Name() {
    return alias_Name;
  }

  public void setAlias_Name(String alias_Name) {
    this.alias_Name = alias_Name;
  }


  public String getAlias_Name_Lower() {
    return alias_Name_Lower;
  }

  public void setAlias_Name_Lower(String alias_Name_Lower) {
    this.alias_Name_Lower = alias_Name_Lower;
  }


  public String getAlias_Uri() {
    return alias_Uri;
  }

  public void setAlias_Uri(String alias_Uri) {
    this.alias_Uri = alias_Uri;
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


  public long getLatest() {
    return latest;
  }

  public void setLatest(long latest) {
    this.latest = latest;
  }

}
