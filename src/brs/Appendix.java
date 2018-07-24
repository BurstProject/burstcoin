package brs;

import brs.crypto.EncryptedData;
import brs.fluxcapacitor.FeatureToggle;
import brs.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;

public interface Appendix {

  int getSize();
  void putBytes(ByteBuffer buffer);
  JSONObject getJSONObject();
  byte getVersion();

  abstract class AbstractAppendix implements Appendix {

    private final byte version;

    AbstractAppendix(JSONObject attachmentData) {
      Long l = (Long) attachmentData.get("version." + getAppendixName());
      version = (byte) (l == null ? 0 : l);
    }

    AbstractAppendix(ByteBuffer buffer, byte transactionVersion) {
        version = (transactionVersion == 0) ? 0 : buffer.get();
    }

    AbstractAppendix(byte version) {
      this.version = version;
    }

    AbstractAppendix(int blockchainHeight) {
      this.version = (byte)(Burst.getFluxCapacitor().isActive(FeatureToggle.DIGITAL_GOODS_STORE, blockchainHeight) ? 1 : 0);
    }

    abstract String getAppendixName();

    @Override
    public final int getSize() {
      return getMySize() + (version > 0 ? 1 : 0);
    }

    abstract int getMySize();

    @Override
    public final void putBytes(ByteBuffer buffer) {
      if (version > 0) {
        buffer.put(version);
      }
      putMyBytes(buffer);
    }

    abstract void putMyBytes(ByteBuffer buffer);

    @Override
    public final JSONObject getJSONObject() {
      JSONObject json = new JSONObject();
      if (version > 0) {
        json.put("version." + getAppendixName(), version);
      }
      putMyJSON(json);
      return json;
    }

    abstract void putMyJSON(JSONObject json);

    @Override
    public final byte getVersion() {
      return version;
    }

    boolean verifyVersion(byte transactionVersion) {
      return transactionVersion == 0 ? version == 0 : version > 0;
    }

    public abstract void validate(Transaction transaction) throws BurstException.ValidationException;

    public abstract void apply(Transaction transaction, Account senderAccount, Account recipientAccount);

  }

  class Message extends AbstractAppendix {

    static Message parse(JSONObject attachmentData) {
      if (attachmentData.get("message") == null) {
        return null;
      }
      return new Message(attachmentData);
    }

    private final byte[] message;
    private final boolean isText;

    public Message(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      int messageLength = buffer.getInt();
      this.isText = messageLength < 0; // ugly hack
      if (messageLength < 0) {
        messageLength &= Integer.MAX_VALUE;
      }
      if (messageLength > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
        throw new BurstException.NotValidException("Invalid arbitrary message length: " + messageLength);
      }
      this.message = new byte[messageLength];
      buffer.get(this.message);
    }

    Message(JSONObject attachmentData) {
      super(attachmentData);
      String messageString = (String)attachmentData.get("message");
      this.isText = Boolean.TRUE.equals(attachmentData.get("messageIsText"));
      this.message = isText ? Convert.toBytes(messageString) : Convert.parseHexString(messageString);
    }

    public Message(byte[] message, int blockchainHeight) {
      super(blockchainHeight);
      this.message = message;
      this.isText = false;
    }

    public Message(String string, int blockchainHeight) {
      super(blockchainHeight);
      this.message = Convert.toBytes(string);
      this.isText = true;
    }

    @Override
    String getAppendixName() {
      return "Message";
    }

    @Override
    int getMySize() {
      return 4 + message.length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putInt(isText ? (message.length | Integer.MIN_VALUE) : message.length);
      buffer.put(message);
    }

    @Override
    void putMyJSON(JSONObject json) {
      json.put("message", isText ? Convert.toString(message) : Convert.toHexString(message));
      json.put("messageIsText", isText);
    }

    @Override
    public void validate(Transaction transaction) throws BurstException.ValidationException {
      if (this.isText && transaction.getVersion() == 0) {
        throw new BurstException.NotValidException("Text messages not yet enabled");
      }
      if (transaction.getVersion() == 0 && transaction.getAttachment() != Attachment.ARBITRARY_MESSAGE) {
        throw new BurstException.NotValidException("Message attachments not enabled for version 0 transactions");
      }
      if (message.length > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
        throw new BurstException.NotValidException("Invalid arbitrary message length: " + message.length);
      }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

    public byte[] getMessage() {
      return message;
    }

    public boolean isText() {
      return isText;
    }
  }

  abstract class AbstractEncryptedMessage extends AbstractAppendix {

    private final EncryptedData encryptedData;
    private final boolean isText;

    private AbstractEncryptedMessage(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      int length = buffer.getInt();
      this.isText = length < 0;
      if (length < 0) {
        length &= Integer.MAX_VALUE;
      }
      this.encryptedData = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_ENCRYPTED_MESSAGE_LENGTH);
    }

    private AbstractEncryptedMessage(JSONObject attachmentJSON, JSONObject encryptedMessageJSON) {
      super(attachmentJSON);
      byte[] data = Convert.parseHexString((String)encryptedMessageJSON.get("data"));
      byte[] nonce = Convert.parseHexString((String)encryptedMessageJSON.get("nonce"));
      this.encryptedData = new EncryptedData(data, nonce);
      this.isText = Boolean.TRUE.equals(encryptedMessageJSON.get("isText"));
    }

    private AbstractEncryptedMessage(EncryptedData encryptedData, boolean isText, int blockchainHeight) {
      super(blockchainHeight);
      this.encryptedData = encryptedData;
      this.isText = isText;
    }

    @Override
    int getMySize() {
      return 4 + encryptedData.getSize();
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putInt(isText ? (encryptedData.getData().length | Integer.MIN_VALUE) : encryptedData.getData().length);
      buffer.put(encryptedData.getData());
      buffer.put(encryptedData.getNonce());
    }

    @Override
    void putMyJSON(JSONObject json) {
      json.put("data", Convert.toHexString(encryptedData.getData()));
      json.put("nonce", Convert.toHexString(encryptedData.getNonce()));
      json.put("isText", isText);
    }

    @Override
    public void validate(Transaction transaction) throws BurstException.ValidationException {
      if (encryptedData.getData().length > Constants.MAX_ENCRYPTED_MESSAGE_LENGTH) {
        throw new BurstException.NotValidException("Max encrypted message length exceeded");
      }
      if ((encryptedData.getNonce().length != 32 && encryptedData.getData().length > 0)
          || (encryptedData.getNonce().length != 0 && encryptedData.getData().length == 0)) {
        throw new BurstException.NotValidException("Invalid nonce length " + encryptedData.getNonce().length);
      }
    }

    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {}

    public final EncryptedData getEncryptedData() {
      return encryptedData;
    }

    public final boolean isText() {
      return isText;
    }

  }

  class EncryptedMessage extends AbstractEncryptedMessage {

    static EncryptedMessage parse(JSONObject attachmentData) throws BurstException.NotValidException {
      if (attachmentData.get("encryptedMessage") == null ) {
        return null;
      }
      return new EncryptedMessage(attachmentData);
    }

    public EncryptedMessage(ByteBuffer buffer, byte transactionVersion) throws BurstException.ValidationException {
      super(buffer, transactionVersion);
    }

    public EncryptedMessage(JSONObject attachmentData) {
      super(attachmentData, (JSONObject)attachmentData.get("encryptedMessage"));
    }

    public EncryptedMessage(EncryptedData encryptedData, boolean isText, int blockchainHeight) {
      super(encryptedData, isText, blockchainHeight);
    }

    @Override
    String getAppendixName() {
      return "EncryptedMessage";
    }

    @Override
    void putMyJSON(JSONObject json) {
      JSONObject encryptedMessageJSON = new JSONObject();
      super.putMyJSON(encryptedMessageJSON);
      json.put("encryptedMessage", encryptedMessageJSON);
    }

    @Override
    public void validate(Transaction transaction) throws BurstException.ValidationException {
      super.validate(transaction);
      if (! transaction.getType().hasRecipient()) {
        throw new BurstException.NotValidException("Encrypted messages cannot be attached to transactions with no recipient");
      }
      if (transaction.getVersion() == 0) {
        throw new BurstException.NotValidException("Encrypted message attachments not enabled for version 0 transactions");
      }
    }

  }

  class EncryptToSelfMessage extends AbstractEncryptedMessage {

    static EncryptToSelfMessage parse(JSONObject attachmentData) throws BurstException.NotValidException {
      if (attachmentData.get("encryptToSelfMessage") == null ) {
        return null;
      }
      return new EncryptToSelfMessage(attachmentData);
    }

    public  EncryptToSelfMessage(ByteBuffer buffer, byte transactionVersion) throws BurstException.ValidationException {
      super(buffer, transactionVersion);
    }

    public EncryptToSelfMessage(JSONObject attachmentData) {
      super(attachmentData, (JSONObject)attachmentData.get("encryptToSelfMessage"));
    }

    public EncryptToSelfMessage(EncryptedData encryptedData, boolean isText, int blockchainHeight) {
      super(encryptedData, isText, blockchainHeight);
    }

    @Override
    String getAppendixName() {
      return "EncryptToSelfMessage";
    }

    @Override
    void putMyJSON(JSONObject json) {
      JSONObject encryptToSelfMessageJSON = new JSONObject();
      super.putMyJSON(encryptToSelfMessageJSON);
      json.put("encryptToSelfMessage", encryptToSelfMessageJSON);
    }

    @Override
    public void validate(Transaction transaction) throws BurstException.ValidationException {
      super.validate(transaction);
      if (transaction.getVersion() == 0) {
        throw new BurstException.NotValidException("Encrypt-to-self message attachments not enabled for version 0 transactions");
      }
    }

  }

  class PublicKeyAnnouncement extends AbstractAppendix {

    static PublicKeyAnnouncement parse(JSONObject attachmentData) {
      if (attachmentData.get("recipientPublicKey") == null) {
        return null;
      }
      return new PublicKeyAnnouncement(attachmentData);
    }

    private final byte[] publicKey;

    public PublicKeyAnnouncement(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.publicKey = new byte[32];
      buffer.get(this.publicKey);
    }

    PublicKeyAnnouncement(JSONObject attachmentData) {
      super(attachmentData);
      this.publicKey = Convert.parseHexString((String)attachmentData.get("recipientPublicKey"));
    }

    public PublicKeyAnnouncement(byte[] publicKey, int blockchainHeight) {
      super(blockchainHeight);
      this.publicKey = publicKey;
    }

    @Override
    String getAppendixName() {
      return "PublicKeyAnnouncement";
    }

    @Override
    int getMySize() {
      return 32;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.put(publicKey);
    }

    @Override
    void putMyJSON(JSONObject json) {
      json.put("recipientPublicKey", Convert.toHexString(publicKey));
    }

    @Override
    public void validate(Transaction transaction) throws BurstException.ValidationException {
      if (! transaction.getType().hasRecipient()) {
        throw new BurstException.NotValidException("PublicKeyAnnouncement cannot be attached to transactions with no recipient");
      }
      if (publicKey.length != 32) {
        throw new BurstException.NotValidException("Invalid recipient public key length: " + Convert.toHexString(publicKey));
      }
      long recipientId = transaction.getRecipientId();
      if (Account.getId(this.publicKey) != recipientId) {
        throw new BurstException.NotValidException("Announced public key does not match recipient accountId");
      }
      if (transaction.getVersion() == 0) {
        throw new BurstException.NotValidException("Public key announcements not enabled for version 0 transactions");
      }
      Account recipientAccount = Account.getAccount(recipientId);
      if (recipientAccount != null && recipientAccount.getPublicKey() != null && ! Arrays.equals(publicKey, recipientAccount.getPublicKey())) {
        throw new BurstException.NotCurrentlyValidException("A different public key for this account has already been announced");
      }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
      if (recipientAccount.setOrVerify(publicKey, transaction.getHeight())) {
        recipientAccount.apply(this.publicKey, transaction.getHeight());
      }
    }

    public byte[] getPublicKey() {
      return publicKey;
    }

  }

}
