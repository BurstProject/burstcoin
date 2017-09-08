package nxt.db.quicksync.pojo;


public class Reward_Recip_Assign {

  private long db_Id;
  private long account_Id;
  private long prev_Recip_Id;
  private long recip_Id;
  private long from_Height;
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


  public long getPrev_Recip_Id() {
    return prev_Recip_Id;
  }

  public void setPrev_Recip_Id(long prev_Recip_Id) {
    this.prev_Recip_Id = prev_Recip_Id;
  }


  public long getRecip_Id() {
    return recip_Id;
  }

  public void setRecip_Id(long recip_Id) {
    this.recip_Id = recip_Id;
  }


  public long getFrom_Height() {
    return from_Height;
  }

  public void setFrom_Height(long from_Height) {
    this.from_Height = from_Height;
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
