package brs.db.sql;

import brs.*;
import brs.schema.tables.records.TransactionRecord;
import brs.util.Convert;
import org.jooq.Cursor;
import org.jooq.Insert;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;

import static brs.schema.Tables.TRANSACTION;

public class SqlTransactionDb implements TransactionDb {

  @Override
  public Transaction findTransaction(long transactionId) {
    try (DSLContext ctx = Db.getDSLContext()) {
      TransactionRecord transactionRecord = ctx.selectFrom(TRANSACTION).where(TRANSACTION.ID.eq(transactionId)).fetchOne();
      return loadTransaction(transactionRecord);
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    } catch (BurstException.ValidationException e) {
      throw new RuntimeException("Transaction already in database, id = " + transactionId + ", does not pass validation!", e);
    }
  }

  @Override
  public Transaction findTransactionByFullHash(String fullHash) {
    try (DSLContext ctx = Db.getDSLContext()) {
      TransactionRecord transactionRecord = ctx.selectFrom(TRANSACTION).where(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(fullHash))).fetchOne();
      return loadTransaction(transactionRecord);
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    } catch (BurstException.ValidationException e) {
      throw new RuntimeException("Transaction already in database, full_hash = " + fullHash + ", does not pass validation!", e);
    }
  }

  @Override
  public boolean hasTransaction(long transactionId) {
    try (DSLContext ctx = Db.getDSLContext()) {
      return ctx.fetchExists(ctx.selectFrom(TRANSACTION).where(TRANSACTION.ID.eq(transactionId)));
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public boolean hasTransactionByFullHash(String fullHash) {
    try (DSLContext ctx = Db.getDSLContext()) {
      return ctx.fetchExists(ctx.selectFrom(TRANSACTION).where(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(fullHash))));
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public TransactionImpl loadTransaction(TransactionRecord tr) throws BurstException.ValidationException {
    if (tr == null) {
      return null;
    }

    ByteBuffer buffer = null;
    if (tr.getAttachmentBytes() != null) {
      buffer = ByteBuffer.wrap(tr.getAttachmentBytes());
      buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    TransactionType transactionType = TransactionType.findTransactionType(tr.getType(), tr.getSubtype());
    TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(tr.getVersion(), tr.getSenderPublicKey(),
            tr.getAmount(), tr.getFee(), tr.getTimestamp(), tr.getDeadline(),
            transactionType.parseAttachment(buffer, tr.getVersion()))
            .referencedTransactionFullHash(tr.getReferencedTransactionFullHash())
            .signature(tr.getSignature())
            .blockId(tr.getBlockId())
            .height(tr.getHeight())
            .id(tr.getId())
            .senderId(tr.getSenderId())
            .blockTimestamp(tr.getBlockTimestamp())
            .fullHash(tr.getFullHash());
    if (transactionType.hasRecipient()) {
      builder.recipientId(tr.getRecipientId());
    }
    if (tr.getHasMessage()) {
      builder.message(new Appendix.Message(buffer, tr.getVersion()));
    }
    if (tr.getHasEncryptedMessage()) {
      builder.encryptedMessage(new Appendix.EncryptedMessage(buffer, tr.getVersion()));
    }
    if (tr.getHasPublicKeyAnnouncement()) {
      builder.publicKeyAnnouncement(new Appendix.PublicKeyAnnouncement(buffer, tr.getVersion()));
    }
    if (tr.getHasEncrypttoselfMessage()) {
      builder.encryptToSelfMessage(new Appendix.EncryptToSelfMessage(buffer, tr.getVersion()));
    }
    if (tr.getVersion() > 0) {
      builder.ecBlockHeight(tr.getEcBlockHeight());
      builder.ecBlockId(Optional.ofNullable(tr.getEcBlockId()).orElse(0L));
    }

    return builder.build();
  }

  @Override
  public TransactionImpl loadTransaction(DSLContext ctx, ResultSet rs) throws BurstException.ValidationException {
    // TODO: remove this method once SqlBlockchainStore no longer requires it
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
        builder.ecBlockId(Optional.ofNullable(ecBlockId).orElse(0L));
      }

      return builder.build();

    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public List<TransactionImpl> findBlockTransactions(long blockId) {
    try (DSLContext ctx = Db.getDSLContext();
         Cursor<TransactionRecord> transactionRecords = ctx.selectFrom(TRANSACTION).
                 where(TRANSACTION.BLOCK_ID.eq(blockId)).fetchLazy()) {
      List<TransactionImpl> list = new ArrayList<>();
      for (TransactionRecord transactionRecord : transactionRecords) {
        list.add(loadTransaction(transactionRecord));
      }
      return list;
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    } catch (BurstException.ValidationException e) {
      throw new RuntimeException("Transaction already in database for block_id = " + Convert.toUnsignedLong(blockId)
              + " does not pass validation!", e);
    }
  }

  private byte[] getAttachmentBytes(Transaction transaction) {
    int bytesLength = 0;
    for (Appendix appendage : transaction.getAppendages()) {
      bytesLength += appendage.getSize();
    }
    if (bytesLength == 0) {
      return null;
    } else {
      ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (Appendix appendage : transaction.getAppendages()) {
        appendage.putBytes(buffer);
      }
      return buffer.array();
    }
  }

  public void saveTransactions(List<TransactionImpl> transactions) {
    try (DSLContext ctx = Db.getDSLContext()) {

      List<Insert<TransactionRecord>> inserts = new ArrayList<>(transactions.size());
      for (TransactionImpl transaction : transactions) {
        Insert<TransactionRecord> insert = ctx.insertInto(TRANSACTION).
                set(TRANSACTION.ID, transaction.getId()).
                set(TRANSACTION.DEADLINE, transaction.getDeadline()).
                set(TRANSACTION.SENDER_PUBLIC_KEY, transaction.getSenderPublicKey()).
                set(TRANSACTION.RECIPIENT_ID, ( transaction.getRecipientId() == 0 ? null : transaction.getRecipientId() )).
                set(TRANSACTION.AMOUNT, transaction.getAmountNQT()).
                set(TRANSACTION.FEE, transaction.getFeeNQT()).
                set(TRANSACTION.REFERENCED_TRANSACTION_FULL_HASH, Convert.parseHexString(transaction.getReferencedTransactionFullHash())).
                set(TRANSACTION.HEIGHT, transaction.getHeight()).
                set(TRANSACTION.BLOCK_ID, transaction.getBlockId()).
                set(TRANSACTION.SIGNATURE, transaction.getSignature()).
                set(TRANSACTION.TIMESTAMP, transaction.getTimestamp()).
                set(TRANSACTION.TYPE, transaction.getType().getType()).
                set(TRANSACTION.SUBTYPE, transaction.getType().getSubtype()).
                set(TRANSACTION.SENDER_ID, transaction.getSenderId()).
                set(TRANSACTION.ATTACHMENT_BYTES, getAttachmentBytes(transaction)).
                set(TRANSACTION.BLOCK_TIMESTAMP, transaction.getBlockTimestamp()).
                set(TRANSACTION.FULL_HASH, Convert.parseHexString(transaction.getFullHash())).
                set(TRANSACTION.VERSION, transaction.getVersion()).
                set(TRANSACTION.HAS_MESSAGE, transaction.getMessage() != null).
                set(TRANSACTION.HAS_ENCRYPTED_MESSAGE, transaction.getEncryptedMessage() != null).
                set(TRANSACTION.HAS_PUBLIC_KEY_ANNOUNCEMENT, transaction.getPublicKeyAnnouncement() != null).
                set(TRANSACTION.HAS_ENCRYPTTOSELF_MESSAGE, transaction.getEncryptToSelfMessage() != null).
                set(TRANSACTION.EC_BLOCK_HEIGHT, transaction.getECBlockHeight()).
                set(TRANSACTION.EC_BLOCK_ID, transaction.getECBlockId() != 0 ? transaction.getECBlockId() : null);

          inserts.add(insert);
      }
      ctx.batch(inserts).execute();
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }
}
