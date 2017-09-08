package nxt.db.quicksync.pojo;


public class Transaction {

  private long db_Id;
  private long id;
  private long deadline;
  private String sender_Public_Key;
  private long recipient_Id;
  private long amount;
  private long fee;
  private long height;
  private long block_Id;
  private String signature;
  private long timestamp;
  private long type;
  private long subtype;
  private long sender_Id;
  private long block_Timestamp;
  private String full_Hash;
  private String referenced_Transaction_Full_Hash;
  private String attachment_Bytes;
  private long version;
  private long has_Message;
  private long has_Encrypted_Message;
  private long has_Public_Key_Announcement;
  private long ec_Block_Height;
  private long ec_Block_Id;
  private long has_Encrypttoself_Message;


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


  public long getDeadline() {
    return deadline;
  }

  public void setDeadline(long deadline) {
    this.deadline = deadline;
  }


  public String getSender_Public_Key() {
    return sender_Public_Key;
  }

  public void setSender_Public_Key(String sender_Public_Key) {
    this.sender_Public_Key = sender_Public_Key;
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


  public long getFee() {
    return fee;
  }

  public void setFee(long fee) {
    this.fee = fee;
  }


  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }


  public long getBlock_Id() {
    return block_Id;
  }

  public void setBlock_Id(long block_Id) {
    this.block_Id = block_Id;
  }


  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }


  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  public long getType() {
    return type;
  }

  public void setType(long type) {
    this.type = type;
  }


  public long getSubtype() {
    return subtype;
  }

  public void setSubtype(long subtype) {
    this.subtype = subtype;
  }


  public long getSender_Id() {
    return sender_Id;
  }

  public void setSender_Id(long sender_Id) {
    this.sender_Id = sender_Id;
  }


  public long getBlock_Timestamp() {
    return block_Timestamp;
  }

  public void setBlock_Timestamp(long block_Timestamp) {
    this.block_Timestamp = block_Timestamp;
  }


  public String getFull_Hash() {
    return full_Hash;
  }

  public void setFull_Hash(String full_Hash) {
    this.full_Hash = full_Hash;
  }


  public String getReferenced_Transaction_Full_Hash() {
    return referenced_Transaction_Full_Hash;
  }

  public void setReferenced_Transaction_Full_Hash(String referenced_Transaction_Full_Hash) {
    this.referenced_Transaction_Full_Hash = referenced_Transaction_Full_Hash;
  }


  public String getAttachment_Bytes() {
    return attachment_Bytes;
  }

  public void setAttachment_Bytes(String attachment_Bytes) {
    this.attachment_Bytes = attachment_Bytes;
  }


  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }


  public long getHas_Message() {
    return has_Message;
  }

  public void setHas_Message(long has_Message) {
    this.has_Message = has_Message;
  }


  public long getHas_Encrypted_Message() {
    return has_Encrypted_Message;
  }

  public void setHas_Encrypted_Message(long has_Encrypted_Message) {
    this.has_Encrypted_Message = has_Encrypted_Message;
  }


  public long getHas_Public_Key_Announcement() {
    return has_Public_Key_Announcement;
  }

  public void setHas_Public_Key_Announcement(long has_Public_Key_Announcement) {
    this.has_Public_Key_Announcement = has_Public_Key_Announcement;
  }


  public long getEc_Block_Height() {
    return ec_Block_Height;
  }

  public void setEc_Block_Height(long ec_Block_Height) {
    this.ec_Block_Height = ec_Block_Height;
  }


  public long getEc_Block_Id() {
    return ec_Block_Id;
  }

  public void setEc_Block_Id(long ec_Block_Id) {
    this.ec_Block_Id = ec_Block_Id;
  }


  public long getHas_Encrypttoself_Message() {
    return has_Encrypttoself_Message;
  }

  public void setHas_Encrypttoself_Message(long has_Encrypttoself_Message) {
    this.has_Encrypttoself_Message = has_Encrypttoself_Message;
  }

}
