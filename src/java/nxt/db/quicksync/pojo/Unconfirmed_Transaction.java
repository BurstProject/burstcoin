package nxt.db.quicksync.pojo;


public class Unconfirmed_Transaction {

  private long db_Id;
  private long id;
  private long expiration;
  private long transaction_Height;
  private long fee_Per_Byte;
  private long timestamp;
  private String transaction_Bytes;
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


  public long getExpiration() {
    return expiration;
  }

  public void setExpiration(long expiration) {
    this.expiration = expiration;
  }


  public long getTransaction_Height() {
    return transaction_Height;
  }

  public void setTransaction_Height(long transaction_Height) {
    this.transaction_Height = transaction_Height;
  }


  public long getFee_Per_Byte() {
    return fee_Per_Byte;
  }

  public void setFee_Per_Byte(long fee_Per_Byte) {
    this.fee_Per_Byte = fee_Per_Byte;
  }


  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  public String getTransaction_Bytes() {
    return transaction_Bytes;
  }

  public void setTransaction_Bytes(String transaction_Bytes) {
    this.transaction_Bytes = transaction_Bytes;
  }


  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }

}
