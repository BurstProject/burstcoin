package nxt.db.quicksync.pojo;


public class Escrow_Decision {

  private long db_Id;
  private long escrow_Id;
  private long account_Id;
  private long decision;
  private long height;
  private long latest;


  public long getDb_Id() {
    return db_Id;
  }

  public void setDb_Id(long db_Id) {
    this.db_Id = db_Id;
  }


  public long getEscrow_Id() {
    return escrow_Id;
  }

  public void setEscrow_Id(long escrow_Id) {
    this.escrow_Id = escrow_Id;
  }


  public long getAccount_Id() {
    return account_Id;
  }

  public void setAccount_Id(long account_Id) {
    this.account_Id = account_Id;
  }


  public long getDecision() {
    return decision;
  }

  public void setDecision(long decision) {
    this.decision = decision;
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
