package brs.db.firebird;

import brs.Appendix;
import brs.NxtException;
import brs.TransactionImpl;
import brs.TransactionType;
import brs.db.sql.DbUtils;
import brs.db.sql.SqlTransactionDb;
import brs.util.Convert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.List;

class FirebirdTransactionDb extends SqlTransactionDb {


    public void saveTransactions(Connection con, List<TransactionImpl> transactions) {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, "
// ac0v
//                + "recipient_id, amount, fee, referenced_transaction_full_hash, height, "
                + "recipient_id, amount, fee, r_t_f_hash, height, "
                + "block_id, signature, \"timestamp\", type, subtype, sender_id, attachment_bytes, "
                + "block_timestamp, full_hash, version, has_message, has_encrypted_message, has_public_key_announcement, "
                + "has_encrypttoself_message, ec_block_height, ec_block_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (TransactionImpl transaction : transactions) {
                int i = 0;
                pstmt.setLong(++i, transaction.getId());
                pstmt.setShort(++i, transaction.getDeadline());
                pstmt.setBytes(++i, transaction.getSenderPublicKey());
                DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getRecipientId());
                pstmt.setLong(++i, transaction.getAmountNQT());
                pstmt.setLong(++i, transaction.getFeeNQT());
                DbUtils.setBytes(pstmt, ++i, Convert.parseHexString(transaction.getReferencedTransactionFullHash()));
                pstmt.setInt(++i, transaction.getHeight());
                pstmt.setLong(++i, transaction.getBlockId());
                if (transaction.getSignature() != null) {
                    pstmt.setBytes(++i, transaction.getSignature());
                } else {
                    pstmt.setNull(++i, Types.BINARY);
                }
                pstmt.setInt(++i, transaction.getTimestamp());
                pstmt.setByte(++i, transaction.getType().getType());
                pstmt.setByte(++i, transaction.getType().getSubtype());
                pstmt.setLong(++i, transaction.getSenderId());
                int bytesLength = 0;
                for (Appendix appendage : transaction.getAppendages()) {
                    bytesLength += appendage.getSize();
                }
                if (bytesLength == 0) {
                    pstmt.setNull(++i, Types.VARBINARY);
                } else {
                    ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    for (Appendix appendage : transaction.getAppendages()) {
                        appendage.putBytes(buffer);
                    }
                    pstmt.setBytes(++i, buffer.array());
                }
                pstmt.setInt(++i, transaction.getBlockTimestamp());
                pstmt.setBytes(++i, Convert.parseHexString(transaction.getFullHash()));
                pstmt.setByte(++i, transaction.getVersion());
                pstmt.setBoolean(++i, transaction.getMessage() != null);
                pstmt.setBoolean(++i, transaction.getEncryptedMessage() != null);
                pstmt.setBoolean(++i, transaction.getPublicKeyAnnouncement() != null);
                pstmt.setBoolean(++i, transaction.getEncryptToSelfMessage() != null);
                pstmt.setInt(++i, transaction.getECBlockHeight());
                DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getECBlockId());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    public TransactionImpl loadTransaction(Connection con, ResultSet rs) throws NxtException.ValidationException {
        try {

            byte type = rs.getByte("type");
            byte subtype = rs.getByte("subtype");
            int timestamp = rs.getInt("timestamp");
            short deadline = rs.getShort("deadline");
            byte[] senderPublicKey = rs.getBytes("sender_public_key");
            long amountNQT = rs.getLong("amount");
            long feeNQT = rs.getLong("fee");
            byte[] referencedTransactionFullHash = rs.getBytes("r_t_f_hash");
            int ecBlockHeight = rs.getInt("ec_block_height");
            long ecBlockId = rs.getLong("ec_block_id");
            byte[] signature = rs.getBytes("signature");
            long blockId = rs.getLong("block_id");
            int height = rs.getInt("height");
            long id = rs.getLong("id");
            long senderId = rs.getLong("sender_id");
            byte[] attachmentBytes = rs.getBytes("attachment_bytes");
            int blockTimestamp = rs.getInt("block_timestamp");
            byte[] fullHash = rs.getBytes("full_hash");
            byte version = rs.getByte("version");

            ByteBuffer buffer = null;
            if (attachmentBytes != null) {
                buffer = ByteBuffer.wrap(attachmentBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey,
                    amountNQT, feeNQT, timestamp, deadline,
                    transactionType.parseAttachment(buffer, version))
                    .referencedTransactionFullHash(referencedTransactionFullHash)
                    .signature(signature)
                    .blockId(blockId)
                    .height(height)
                    .id(id)
                    .senderId(senderId)
                    .blockTimestamp(blockTimestamp)
                    .fullHash(fullHash);
            if (transactionType.hasRecipient()) {
                long recipientId = rs.getLong("recipient_id");
                if (!rs.wasNull()) {
                    builder.recipientId(recipientId);
                }
            }
            if (rs.getBoolean("has_message")) {
                builder.message(new Appendix.Message(buffer, version));
            }
            if (rs.getBoolean("has_encrypted_message")) {
                builder.encryptedMessage(new Appendix.EncryptedMessage(buffer, version));
            }
            if (rs.getBoolean("has_public_key_announcement")) {
                builder.publicKeyAnnouncement(new Appendix.PublicKeyAnnouncement(buffer, version));
            }
            if (rs.getBoolean("has_encrypttoself_message")) {
                builder.encryptToSelfMessage(new Appendix.EncryptToSelfMessage(buffer, version));
            }
            if (version > 0) {
                builder.ecBlockHeight(ecBlockHeight);
                builder.ecBlockId(ecBlockId);
            }

            return builder.build();

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
