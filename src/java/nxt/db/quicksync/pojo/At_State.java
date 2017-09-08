package nxt.db.quicksync.pojo;


public class At_State {

  private long db_Id;
  private long at_Id;
  private String state;
  private long prev_Height;
  private long next_Height;
  private long sleep_Between;
  private long prev_Balance;
  private long freeze_When_Same_Balance;
  private long min_Activate_Amount;
  private long height;
  private long latest;


  public long getDb_Id() {
    return db_Id;
  }

  public void setDb_Id(long db_Id) {
    this.db_Id = db_Id;
  }


  public long getAt_Id() {
    return at_Id;
  }

  public void setAt_Id(long at_Id) {
    this.at_Id = at_Id;
  }


  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }


  public long getPrev_Height() {
    return prev_Height;
  }

  public void setPrev_Height(long prev_Height) {
    this.prev_Height = prev_Height;
  }


  public long getNext_Height() {
    return next_Height;
  }

  public void setNext_Height(long next_Height) {
    this.next_Height = next_Height;
  }


  public long getSleep_Between() {
    return sleep_Between;
  }

  public void setSleep_Between(long sleep_Between) {
    this.sleep_Between = sleep_Between;
  }


  public long getPrev_Balance() {
    return prev_Balance;
  }

  public void setPrev_Balance(long prev_Balance) {
    this.prev_Balance = prev_Balance;
  }


  public long getFreeze_When_Same_Balance() {
    return freeze_When_Same_Balance;
  }

  public void setFreeze_When_Same_Balance(long freeze_When_Same_Balance) {
    this.freeze_When_Same_Balance = freeze_When_Same_Balance;
  }


  public long getMin_Activate_Amount() {
    return min_Activate_Amount;
  }

  public void setMin_Activate_Amount(long min_Activate_Amount) {
    this.min_Activate_Amount = min_Activate_Amount;
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
