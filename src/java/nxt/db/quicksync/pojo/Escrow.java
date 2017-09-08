package nxt.db.quicksync.pojo;


public class Escrow {

  private long db_Id;
  private long id;
  private long sender_Id;
  private long recipient_Id;
  private long amount;
  private Long required_Signers;
  private long deadline;
  private long deadline_Action;
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


  public long getRequired_Signers() {
    return required_Signers;
  }

  public void setRequired_Signers(long required_Signers) {
    this.required_Signers = required_Signers;
  }


  public long getDeadline() {
    return deadline;
  }

  public void setDeadline(long deadline) {
    this.deadline = deadline;
  }


  public long getDeadline_Action() {
    return deadline_Action;
  }

  public void setDeadline_Action(long deadline_Action) {
    this.deadline_Action = deadline_Action;
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
