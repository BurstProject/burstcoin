package nxt;

import org.json.simple.JSONObject;

import java.util.List;

public interface Transaction extends Comparable<Transaction> {

    public static interface Builder {

        Builder recipientId(Long recipientId);

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder message(Appendix.Message message);

        Builder encryptedMessage(Appendix.EncryptedMessage encryptedMessage);

        Builder encryptToSelfMessage(Appendix.EncryptToSelfMessage encryptToSelfMessage);

        Builder publicKeyAnnouncement(Appendix.PublicKeyAnnouncement publicKeyAnnouncement);

        Transaction build() throws NxtException.NotValidException;

    }

    Long getId();

    String getStringId();

    Long getSenderId();

    byte[] getSenderPublicKey();

    Long getRecipientId();

    int getHeight();

    Long getBlockId();

    Block getBlock();

    int getTimestamp();

    int getBlockTimestamp();

    short getDeadline();

    int getExpiration();

    long getAmountNQT();

    long getFeeNQT();

    String getReferencedTransactionFullHash();

    byte[] getSignature();

    String getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    void sign(String secretPhrase);

    boolean verifySignature();

    void validate() throws NxtException.ValidationException;

    byte[] getBytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    byte getVersion();

    Appendix.Message getMessage();

    Appendix.EncryptedMessage getEncryptedMessage();

    Appendix.EncryptToSelfMessage getEncryptToSelfMessage();

    List<? extends Appendix> getAppendages();

    /*
    Collection<TransactionType> getPhasingTransactionTypes();

    Collection<TransactionType> getPhasedTransactionTypes();
    */

    int getECBlockHeight();

    Long getECBlockId();

}
