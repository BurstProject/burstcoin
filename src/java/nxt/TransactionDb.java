package nxt;

import nxt.at.AT_API_Helper;
import nxt.at.AT_Transaction;
import nxt.crypto.Crypto;
import nxt.db.Db;
import nxt.db.DbUtils;
import nxt.util.Convert;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

final class TransactionDb {

    static Transaction findTransaction(long transactionId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return loadTransaction(con, rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, id = " + transactionId + ", does not pass validation!", e);
        }
    }

    static Transaction findTransactionByFullHash(String fullHash) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE full_hash = ?")) {
            pstmt.setBytes(1, Convert.parseHexString(fullHash));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return loadTransaction(con, rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, full_hash = " + fullHash + ", does not pass validation!", e);
        }
    }

    static boolean hasTransaction(long transactionId) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static boolean hasTransactionByFullHash(String fullHash) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM transaction WHERE full_hash = ?")) {
            pstmt.setBytes(1, Convert.parseHexString(fullHash));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static TransactionImpl loadTransaction(Connection con, ResultSet rs) throws NxtException.ValidationException {
        try {

            byte type = rs.getByte("type");
            byte subtype = rs.getByte("subtype");
            int timestamp = rs.getInt("timestamp");
            short deadline = rs.getShort("deadline");
            byte[] senderPublicKey = rs.getBytes("sender_public_key");
            long amountNQT = rs.getLong("amount");
            long feeNQT = rs.getLong("fee");
            byte[] referencedTransactionFullHash = rs.getBytes("referenced_transaction_full_hash");
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
                if (! rs.wasNull()) {
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

    static List<TransactionImpl> findBlockTransactions(long blockId) {
    	try (Connection con = Db.getConnection();
    			PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE block_id = ? AND signature IS NOT NULL ORDER BY id")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
            	List<TransactionImpl> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(loadTransaction(con, rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (NxtException.ValidationException e) {
            throw new RuntimeException("Transaction already in database for block_id = " + Convert.toUnsignedLong(blockId)
                    + " does not pass validation!", e);
        }
    }

    static void saveTransactions(Connection con, List<TransactionImpl> transactions) {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, "
                + "recipient_id, amount, fee, referenced_transaction_full_hash, height, "
                + "block_id, signature, timestamp, type, subtype, sender_id, attachment_bytes, "
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

    /*static void saveTransactions(Connection con, AT at, Block block) {
		try {
			for ( AT_Transaction transaction : at.getTransactions() ) {
				try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, "
						+ "recipient_id, amount, fee, referenced_transaction_full_hash, height, "
						+ "block_id, signature, timestamp, type, subtype, sender_id, attachment_bytes, "
						+ "block_timestamp, full_hash, version, has_message, has_encrypted_message, has_public_key_announcement, "
						+ "has_encrypttoself_message, ec_block_height, ec_block_id) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
					int i = 0;

					byte[] signature = new byte[ 64 ];
					byte[] hash;

					Account senderAccount = Account.getAccount( AT_API_Helper.getLong( at.getId() ) );
					Account recipientAccount = Account.getAccount( AT_API_Helper.getLong( transaction.getRecipientId() ) ); 

					Long totalAmount = transaction.getAmount();

					if ( !( senderAccount.getUnconfirmedBalanceNQT() < totalAmount ) )
					{
						senderAccount.addToUnconfirmedBalanceNQT( -totalAmount );
						senderAccount.addToBalanceNQT( -totalAmount );

			            recipientAccount.addToBalanceAndUnconfirmedBalanceNQT( totalAmount );


						ByteBuffer b = ByteBuffer.allocate( ( 8 + 8 ) );
						b.order( ByteOrder.LITTLE_ENDIAN );
						b.put( transaction.getRecipientId() );
						b.putLong( transaction.getAmount() );
						//if (useNQT()) {
							byte[] data = at.getBytes();
							//byte[] data = zeroSignature(getBytes());
							byte[] signatureHash = Crypto.sha256().digest(signature);
							MessageDigest digest = Crypto.sha256();
							digest.update(data);
							digest.update(b.array());
							digest.update(block.getBlockSignature());
							hash = digest.digest(signatureHash);
							BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
							Long id = bigInteger.longValue();
							//String stringId = bigInteger.toString();
							String fullHash = Convert.toHexString(hash);


							pstmt.setLong(++i, id);
							pstmt.setShort(++i, (short) 1440 );
							pstmt.setBytes(++i, new byte[ 32 ]);
							if ( transaction.getRecipientId() != null ) {
								pstmt.setLong(++i, AT_API_Helper.getLong( transaction.getRecipientId() ));
							} else {
								pstmt.setNull(++i, Types.BIGINT);
							}
							pstmt.setLong(++i, transaction.getAmount() );
							pstmt.setLong(++i, 0L );
							//if (transaction.getReferencedTransactionFullHash() != null) {
								//    pstmt.setBytes(++i, Convert.parseHexString(transaction.getReferencedTransactionFullHash()));
							//} else {
							pstmt.setNull(++i, Types.BINARY);
							//}
							pstmt.setInt(++i, block.getHeight() );
							pstmt.setLong(++i, block.getId() );
							pstmt.setBytes(++i, new byte[ 64 ]);
							pstmt.setInt(++i, block.getTimestamp());
							pstmt.setByte(++i, (byte)5);
							pstmt.setByte(++i, (byte)1);
							pstmt.setLong(++i, AT_API_Helper.getLong( at.getId() ) );
							int bytesLength = 0;

							Attachment appendage = Attachment.AT_PAYMENT;
							bytesLength += appendage.getSize();
							if (bytesLength == 0) {
								pstmt.setNull(++i, Types.VARBINARY);
							} else {
								ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
								buffer.order(ByteOrder.LITTLE_ENDIAN);
								appendage.putBytes(buffer);
								pstmt.setBytes(++i, buffer.array());
							}
							pstmt.setInt(++i, block.getTimestamp());
							pstmt.setBytes(++i, Convert.parseHexString(fullHash));
							pstmt.setByte(++i, (byte)1);
							pstmt.setBoolean(++i, false );
							pstmt.setBoolean(++i, false );
							pstmt.setBoolean(++i, false );
							pstmt.setBoolean(++i, false );
							Block ecBlock = EconomicClustering.getECBlock(block.getTimestamp());
							pstmt.setInt(++i, ecBlock.getHeight());
							if (block.getId() != 0L) {
								pstmt.setLong(++i, ecBlock.getId());
							} else {
								pstmt.setNull(++i, Types.BIGINT);
							}
							pstmt.executeUpdate();
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}*/
}
