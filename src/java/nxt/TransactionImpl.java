package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

final class TransactionImpl implements Transaction {

    private final short deadline;
    private final byte[] senderPublicKey;
    private final Long recipientId;
    private final long amountNQT;
    private final long feeNQT;
    private final String referencedTransactionFullHash;
    private final TransactionType type;

    private int height = Integer.MAX_VALUE;
    private Long blockId;
    private volatile Block block;
    private volatile byte[] signature;
    private final int timestamp;
    private int blockTimestamp = -1;
    private Attachment attachment;
    private volatile Long id;
    private volatile String stringId = null;
    private volatile Long senderId;
    private volatile String fullHash;

    TransactionImpl(TransactionType type, int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                    long amountNQT, long feeNQT, String referencedTransactionFullHash, byte[] signature) throws NxtException.ValidationException {

        if ((deadline < 1 || feeNQT < Constants.ONE_NXT)
                || feeNQT > Constants.MAX_BALANCE_NQT
                || amountNQT < 0
                || amountNQT > Constants.MAX_BALANCE_NQT
                || type == null) {
            throw new NxtException.ValidationException("Invalid transaction parameters:\n type: " + type + ", timestamp: " + timestamp
                    + ", deadline: " + deadline + ", fee: " + feeNQT + ", amount: " + amountNQT);
        }

        this.timestamp = timestamp;
        this.deadline = deadline;
        this.senderPublicKey = senderPublicKey;
        this.recipientId = recipientId;
        this.amountNQT = amountNQT;
        this.feeNQT = feeNQT;
        this.referencedTransactionFullHash = referencedTransactionFullHash;
        this.signature = signature;
        this.type = type;
    }

    TransactionImpl(TransactionType type, int timestamp, short deadline, byte[] senderPublicKey, Long recipientId,
                    long amountNQT, long feeNQT, byte[] referencedTransactionFullHash, byte[] signature, Long blockId, int height,
                    Long id, Long senderId, int blockTimestamp, byte[] fullHash)
            throws NxtException.ValidationException {
        this(type, timestamp, deadline, senderPublicKey, recipientId, amountNQT, feeNQT,
                referencedTransactionFullHash == null ? null : Convert.toHexString(referencedTransactionFullHash),
                signature);
        this.blockId = blockId;
        this.height = height;
        this.id = id;
        this.senderId = senderId;
        this.blockTimestamp = blockTimestamp;
        this.fullHash = fullHash == null ? null : Convert.toHexString(fullHash);
    }

    @Override
    public short getDeadline() {
        return deadline;
    }

    @Override
    public byte[] getSenderPublicKey() {
        return senderPublicKey;
    }

    @Override
    public Long getRecipientId() {
        return recipientId;
    }

    @Override
    public long getAmountNQT() {
        return amountNQT;
    }

    @Override
    public long getFeeNQT() {
        return feeNQT;
    }

    @Override
    public String getReferencedTransactionFullHash() {
        return referencedTransactionFullHash;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public Long getBlockId() {
        return blockId;
    }

    @Override
    public Block getBlock() {
        if (block == null) {
            block = BlockDb.findBlock(blockId);
        }
        return block;
    }

    void setBlock(Block block) {
        this.block = block;
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.blockTimestamp = block.getTimestamp();
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    @Override
    public int getExpiration() {
        return timestamp + deadline * 60;
    }

    @Override
    public Attachment getAttachment() {
        return attachment;
    }

    void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    @Override
    public Long getId() {
        if (id == null) {
            if (signature == null) {
                throw new IllegalStateException("Transaction is not signed yet");
            }
            byte[] hash;
            if (useNQT()) {
                byte[] data = zeroSignature(getBytes());
                byte[] signatureHash = Crypto.sha256().digest(signature);
                MessageDigest digest = Crypto.sha256();
                digest.update(data);
                hash = digest.digest(signatureHash);
            } else {
                hash = Crypto.sha256().digest(getBytes());
            }
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
            fullHash = Convert.toHexString(hash);
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Convert.toUnsignedLong(id);
            }
        }
        return stringId;
    }

    @Override
    public String getFullHash() {
        if (fullHash == null) {
            getId();
        }
        return fullHash;
    }

    @Override
    public Long getSenderId() {
        if (senderId == null) {
            senderId = Account.getId(senderPublicKey);
        }
        return senderId;
    }

    @Override
    public int compareTo(Transaction o) {

        if (height < o.getHeight()) {
            return -1;
        }
        if (height > o.getHeight()) {
            return 1;
        }
        // equivalent to: fee * 1048576L / getSize() > o.fee * 1048576L / o.getSize()
        if (Convert.safeMultiply(feeNQT, ((TransactionImpl)o).getSize()) > Convert.safeMultiply(o.getFeeNQT(), getSize())) {
            return -1;
        }
        if (Convert.safeMultiply(feeNQT, ((TransactionImpl)o).getSize()) < Convert.safeMultiply(o.getFeeNQT(), getSize())) {
            return 1;
        }
        if (timestamp < o.getTimestamp()) {
            return -1;
        }
        if (timestamp > o.getTimestamp()) {
            return 1;
        }
        if (getId() < o.getId()) {
            return -1;
        }
        if (getId() > o.getId()) {
            return 1;
        }
        return 0;

    }

    @Override
    public byte[] getBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(type.getType());
        buffer.put(type.getSubtype());
        buffer.putInt(timestamp);
        buffer.putShort(deadline);
        buffer.put(senderPublicKey);
        buffer.putLong(Convert.nullToZero(recipientId));
        if (useNQT()) {
            buffer.putLong(amountNQT);
            buffer.putLong(feeNQT);
            if (referencedTransactionFullHash != null) {
                buffer.put(Convert.parseHexString(referencedTransactionFullHash));
            } else {
                buffer.put(new byte[32]);
            }
        } else {
            buffer.putInt((int)(amountNQT / Constants.ONE_NXT));
            buffer.putInt((int)(feeNQT / Constants.ONE_NXT));
            if (referencedTransactionFullHash != null) {
                buffer.putLong(Convert.fullHashToId(Convert.parseHexString(referencedTransactionFullHash)));
            } else {
                buffer.putLong(0L);
            }
        }
        buffer.put(signature != null ? signature : new byte[64]);
        if (attachment != null) {
            buffer.put(attachment.getBytes());
        }
        return buffer.array();
    }

    @Override
    public byte[] getUnsignedBytes() {
        return zeroSignature(getBytes());
    }

    /*
    @Override
    public Collection<TransactionType> getPhasingTransactionTypes() {
        return getType().getPhasingTransactionTypes();
    }

    @Override
    public Collection<TransactionType> getPhasedTransactionTypes() {
        return getType().getPhasedTransactionTypes();
    }
    */

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("type", type.getType());
        json.put("subtype", type.getSubtype());
        json.put("timestamp", timestamp);
        json.put("deadline", deadline);
        json.put("senderPublicKey", Convert.toHexString(senderPublicKey));
        json.put("recipient", Convert.toUnsignedLong(recipientId));
        json.put("amountNQT", amountNQT);
        json.put("feeNQT", feeNQT);
        if (referencedTransactionFullHash != null) {
            json.put("referencedTransactionFullHash", referencedTransactionFullHash);
        }
        json.put("signature", Convert.toHexString(signature));
        if (attachment != null) {
            json.put("attachment", attachment.getJSONObject());
        }
        return json;
    }

    @Override
    public void sign(String secretPhrase) {
        if (signature != null) {
            throw new IllegalStateException("Transaction already signed");
        }
        signature = Crypto.sign(getBytes(), secretPhrase);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransactionImpl && this.getId().equals(((Transaction)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public boolean verify() {
        Account account = Account.getAccount(getSenderId());
        if (account == null) {
            return false;
        }
        if (signature == null) {
            return false;
        }
        byte[] data = zeroSignature(getBytes());
        return Crypto.verify(signature, data, senderPublicKey, useNQT()) && account.setOrVerify(senderPublicKey, this.getHeight());
    }

    int getSize() {
        return signatureOffset() + 64  +  (attachment == null ? 0 : attachment.getSize());
    }

    private int signatureOffset() {
        return 1 + 1 + 4 + 2 + 32 + 8 + (useNQT() ? 8 + 8 + 32 : 4 + 4 + 8);
    }

    private boolean useNQT() {
        return this.height > Constants.NQT_BLOCK
                && (this.height < Integer.MAX_VALUE
                || Nxt.getBlockchain().getHeight() >= Constants.NQT_BLOCK);
    }

    private byte[] zeroSignature(byte[] data) {
        int start = signatureOffset();
        for (int i = start; i < start + 64; i++) {
            data[i] = 0;
        }
        return data;
    }

    @Override
    public void validateAttachment() throws NxtException.ValidationException {
        type.validateAttachment(this);
    }

    // returns false iff double spending
    boolean applyUnconfirmed() {
        Account senderAccount = Account.getAccount(getSenderId());
        if (senderAccount == null) {
            return false;
        }
        synchronized(senderAccount) {
            return type.applyUnconfirmed(this, senderAccount);
        }
    }

    void apply() {
        Account senderAccount = Account.getAccount(getSenderId());
        senderAccount.apply(senderPublicKey, this.getHeight());
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null) {
            recipientAccount = Account.addOrGetAccount(recipientId);
        }
        type.apply(this, senderAccount, recipientAccount);
    }

    void undoUnconfirmed() {
        Account senderAccount = Account.getAccount(getSenderId());
        type.undoUnconfirmed(this, senderAccount);
    }

    // NOTE: when undo is called, lastBlock has already been set to the previous block
    void undo() throws TransactionType.UndoNotSupportedException {
        Account senderAccount = Account.getAccount(senderId);
        senderAccount.undo(this.getHeight());
        Account recipientAccount = Account.getAccount(recipientId);
        type.undo(this, senderAccount, recipientAccount);
    }

    /*
    void updateTotals(Map<Long,Long> accumulatedAmounts, Map<Long,Map<Long,Long>> accumulatedAssetQuantities) {
        Long senderId = getSenderId();
        Long accumulatedAmount = accumulatedAmounts.get(senderId);
        if (accumulatedAmount == null) {
            accumulatedAmount = 0L;
        }
        accumulatedAmounts.put(senderId, Convert.safeAdd(accumulatedAmount, Convert.safeAdd(amountNQT, feeNQT)));
        type.updateTotals(this, accumulatedAmounts, accumulatedAssetQuantities, accumulatedAmount);
    }
    */

    boolean isDuplicate(Map<TransactionType, Set<String>> duplicates) {
        return type.isDuplicate(this, duplicates);
    }

}
