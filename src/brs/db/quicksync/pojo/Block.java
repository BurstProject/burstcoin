package brs.db.quicksync.pojo;


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

}
