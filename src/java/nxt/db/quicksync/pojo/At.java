package nxt.db.quicksync.pojo;


public class At {

  private long db_Id;
  private long id;
  private long creator_Id;
  private String name;
  private String description;
  private long version;
  private long csize;
  private long dsize;
  private long c_User_Stack_Bytes;
  private long c_Call_Stack_Bytes;
  private long creation_Height;
  private String ap_Code;
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


  public long getCreator_Id() {
    return creator_Id;
  }

  public void setCreator_Id(long creator_Id) {
    this.creator_Id = creator_Id;
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


  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }


  public long getCsize() {
    return csize;
  }

  public void setCsize(long csize) {
    this.csize = csize;
  }


  public long getDsize() {
    return dsize;
  }

  public void setDsize(long dsize) {
    this.dsize = dsize;
  }


  public long getC_User_Stack_Bytes() {
    return c_User_Stack_Bytes;
  }

  public void setC_User_Stack_Bytes(long c_User_Stack_Bytes) {
    this.c_User_Stack_Bytes = c_User_Stack_Bytes;
  }


  public long getC_Call_Stack_Bytes() {
    return c_Call_Stack_Bytes;
  }

  public void setC_Call_Stack_Bytes(long c_Call_Stack_Bytes) {
    this.c_Call_Stack_Bytes = c_Call_Stack_Bytes;
  }


  public long getCreation_Height() {
    return creation_Height;
  }

  public void setCreation_Height(long creation_Height) {
    this.creation_Height = creation_Height;
  }


  public String getAp_Code() {
    return ap_Code;
  }

  public void setAp_Code(String ap_Code) {
    this.ap_Code = ap_Code;
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
