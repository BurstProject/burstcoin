package nxt.db.quicksync.pojo;




public class Block {

  private long db_Id;
  private long id;
  private long version;
  private long timestamp;
  private Long previous_Block_Id;
  private long total_Amount;
  private long total_Fee;
  private long payload_Length;
  private byte[] generator_Public_Key;
  private byte[] previous_Block_Hash;
  private byte[] cumulative_Difficulty;
  private long base_Target;
  private Long next_Block_Id;
  private long height;
  private byte[] generation_Signature;
  private byte[] block_Signature;
  private byte[] payload_Hash;
  private long generator_Id;
  private long nonce;
  private byte[] ats;

  public long getDb_Id ()
  {
    return db_Id;
  }

  public void setDb_Id (long db_Id)
  {
    this.db_Id = db_Id;
  }

  public long getId ()
  {
    return id;
  }

  public void setId (long id)
  {
    this.id = id;
  }

  public long getVersion ()
  {
    return version;
  }

  public void setVersion (long version)
  {
    this.version = version;
  }

  public long getTimestamp ()
  {
    return timestamp;
  }

  public void setTimestamp (long timestamp)
  {
    this.timestamp = timestamp;
  }

  public Long getPrevious_Block_Id ()
  {
    return previous_Block_Id;
  }

  public void setPrevious_Block_Id (Long previous_Block_Id)
  {
    this.previous_Block_Id = previous_Block_Id;
  }

  public long getTotal_Amount ()
  {
    return total_Amount;
  }

  public void setTotal_Amount (long total_Amount)
  {
    this.total_Amount = total_Amount;
  }

  public long getTotal_Fee ()
  {
    return total_Fee;
  }

  public void setTotal_Fee (long total_Fee)
  {
    this.total_Fee = total_Fee;
  }

  public long getPayload_Length ()
  {
    return payload_Length;
  }

  public void setPayload_Length (long payload_Length)
  {
    this.payload_Length = payload_Length;
  }

  public byte[] getGenerator_Public_Key ()
  {
    return generator_Public_Key;
  }

  public void setGenerator_Public_Key (byte[] generator_Public_Key)
  {
    this.generator_Public_Key = generator_Public_Key;
  }

  public byte[] getPrevious_Block_Hash ()
  {
    return previous_Block_Hash;
  }

  public void setPrevious_Block_Hash (byte[] previous_Block_Hash)
  {
    this.previous_Block_Hash = previous_Block_Hash;
  }

  public byte[] getCumulative_Difficulty ()
  {
    return cumulative_Difficulty;
  }

  public void setCumulative_Difficulty (byte[] cumulative_Difficulty)
  {
    this.cumulative_Difficulty = cumulative_Difficulty;
  }

  public long getBase_Target ()
  {
    return base_Target;
  }

  public void setBase_Target (long base_Target)
  {
    this.base_Target = base_Target;
  }

  public Long getNext_Block_Id ()
  {
    return next_Block_Id;
  }

  public void setNext_Block_Id (Long next_Block_Id)
  {
    this.next_Block_Id = next_Block_Id;
  }

  public long getHeight ()
  {
    return height;
  }

  public void setHeight (long height)
  {
    this.height = height;
  }

  public byte[] getGeneration_Signature ()
  {
    return generation_Signature;
  }

  public void setGeneration_Signature (byte[] generation_Signature)
  {
    this.generation_Signature = generation_Signature;
  }

  public byte[] getBlock_Signature ()
  {
    return block_Signature;
  }

  public void setBlock_Signature (byte[] block_Signature)
  {
    this.block_Signature = block_Signature;
  }

  public byte[] getPayload_Hash ()
  {
    return payload_Hash;
  }

  public void setPayload_Hash (byte[] payload_Hash)
  {
    this.payload_Hash = payload_Hash;
  }

  public long getGenerator_Id ()
  {
    return generator_Id;
  }

  public void setGenerator_Id (long generator_Id)
  {
    this.generator_Id = generator_Id;
  }

  public long getNonce ()
  {
    return nonce;
  }

  public void setNonce (long nonce)
  {
    this.nonce = nonce;
  }

  public byte[] getAts ()
  {
    return ats;
  }

  public void setAts (byte[] ats)
  {
    this.ats = ats;
  }
}
