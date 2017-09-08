package nxt.db.quicksync.pojo;


public class Purchase_Public_Feedback {

  private long db_Id;
  private long id;
  private String public_Feedback;
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


  public String getPublic_Feedback() {
    return public_Feedback;
  }

  public void setPublic_Feedback(String public_Feedback) {
    this.public_Feedback = public_Feedback;
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
