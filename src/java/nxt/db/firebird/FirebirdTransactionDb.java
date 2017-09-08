package nxt.db.firebird;

import nxt.Appendix;
import nxt.TransactionImpl;
import nxt.db.sql.DbUtils;
import nxt.db.sql.SqlTransactionDb;
import nxt.util.Convert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
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

}
