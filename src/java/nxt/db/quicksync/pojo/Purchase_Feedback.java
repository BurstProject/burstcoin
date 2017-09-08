package nxt.db.quicksync.pojo;


public class Purchase_Feedback {

  private long db_Id;
  private long id;
  private String feedback_Data;
  private String feedback_Nonce;
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


  public String getFeedback_Data() {
    return feedback_Data;
  }

  public void setFeedback_Data(String feedback_Data) {
    this.feedback_Data = feedback_Data;
  }


  public String getFeedback_Nonce() {
    return feedback_Nonce;
  }

  public void setFeedback_Nonce(String feedback_Nonce) {
    this.feedback_Nonce = feedback_Nonce;
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
