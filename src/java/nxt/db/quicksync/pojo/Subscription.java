package nxt.db.quicksync.pojo;


public class Subscription {

  private long db_Id;
  private long id;
  private long sender_Id;
  private long recipient_Id;
  private long amount;
  private long frequency;
  private long time_Next;
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


  public long getAmount() {
    return amount;
  }

  public void setAmount(long amount) {
    this.amount = amount;
  }


  public long getFrequency() {
    return frequency;
  }

  public void setFrequency(long frequency) {
    this.frequency = frequency;
  }


  public long getTime_Next() {
    return time_Next;
  }

  public void setTime_Next(long time_Next) {
    this.time_Next = time_Next;
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
