package nxt.db.quicksync.pojo;




public class Account {

  private long db_Id;
  private long id;
  private long creation_Height;
  private byte[] public_Key;
  private long key_Height;
  private long balance;
  private long unconfirmed_Balance;
  private long forged_Balance;
  private String name;
  private String description;
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


  public long getCreation_Height() {
    return creation_Height;
  }

  public void setCreation_Height(long creation_Height) {
    this.creation_Height = creation_Height;
  }

  public byte[] getPublic_Key() {
    return public_Key;
  }

  public void setPublic_Key(byte[] public_Key) {
    this.public_Key = public_Key;
  }

  public long getKey_Height() {
    return key_Height;
  }

  public void setKey_Height(long key_Height) {
    this.key_Height = key_Height;
  }


  public long getBalance() {
    return balance;
  }

  public void setBalance(long balance) {
    this.balance = balance;
  }


  public long getUnconfirmed_Balance() {
    return unconfirmed_Balance;
  }

  public void setUnconfirmed_Balance(long unconfirmed_Balance) {
    this.unconfirmed_Balance = unconfirmed_Balance;
  }


  public long getForged_Balance() {
    return forged_Balance;
  }

  public void setForged_Balance(long forged_Balance) {
    this.forged_Balance = forged_Balance;
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

  @Override
  public String toString() {
    return "Account{" +
            "db_Id=" + db_Id +
            ", id=" + id +
            ", creation_Height=" + creation_Height +
            ", public_Key='" + public_Key + '\'' +
            ", key_Height=" + key_Height +
            ", balance=" + balance +
            ", unconfirmed_Balance=" + unconfirmed_Balance +
            ", forged_Balance=" + forged_Balance +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", height=" + height +
            ", latest=" + latest +
            '}';
  }
}
