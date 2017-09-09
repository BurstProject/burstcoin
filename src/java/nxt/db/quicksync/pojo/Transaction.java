package nxt.db.quicksync.pojo;


public class Transaction {

    private long db_Id;
    private long id;
    private long deadline;
    private byte[] sender_Public_Key;
    private Long recipient_Id;
    private long amount;
    private long fee;
    private long height;
    private long block_Id;
    private byte[] signature;
    private long timestamp;
    private long type;
    private long subtype;
    private long sender_Id;
    private long block_Timestamp;
    private byte[] full_Hash;
    private byte[] referenced_Transaction_Full_Hash;
    private byte[] attachment_Bytes;
    private long version;
    private long has_Message;
    private long has_Encrypted_Message;
    private long has_Public_Key_Announcement;
    private long ec_Block_Height;
    private long ec_Block_Id;
    private long has_Encrypttoself_Message;


}
