package brs.db.sql;

import brs.AssetTransfer;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.store.AssetTransferStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jooq.DSLContext;

import static brs.schema.Tables.ASSET_TRANSFER;

public class SqlAssetTransferStore implements AssetTransferStore {

  protected static final BurstKey.LongKeyFactory<AssetTransfer> transferDbKeyFactory = new DbKey.LongKeyFactory<AssetTransfer>("id") {

      @Override
      public BurstKey newKey(AssetTransfer assetTransfer) {
        return assetTransfer.dbKey;
      }
    };
  private final EntitySqlTable<AssetTransfer> assetTransferTable = new EntitySqlTable<AssetTransfer>("asset_transfer", brs.schema.Tables.ASSET_TRANSFER, transferDbKeyFactory) {

      @Override
      protected AssetTransfer load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlAssetTransfer(rs);
      }

      @Override
      protected void save(DSLContext ctx, AssetTransfer assetTransfer) throws SQLException {
        saveAssetTransfer(assetTransfer);
      }
    };

  private void saveAssetTransfer(AssetTransfer assetTransfer) throws SQLException {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      ctx.insertInto(
        ASSET_TRANSFER,
        ASSET_TRANSFER.ID, ASSET_TRANSFER.ASSET_ID, ASSET_TRANSFER.SENDER_ID, ASSET_TRANSFER.RECIPIENT_ID,
        ASSET_TRANSFER.QUANTITY, ASSET_TRANSFER.TIMESTAMP, ASSET_TRANSFER.HEIGHT
      ).values(
        assetTransfer.getId(), assetTransfer.getAssetId(), assetTransfer.getSenderId(), assetTransfer.getRecipientId(),
        assetTransfer.getQuantityQNT(), assetTransfer.getTimestamp(), assetTransfer.getHeight()
      ).execute();
    }
  }


  @Override
  public EntitySqlTable<AssetTransfer> getAssetTransferTable() {
    return assetTransferTable;
  }

  @Override
  public BurstKey.LongKeyFactory<AssetTransfer> getTransferDbKeyFactory() {
    return transferDbKeyFactory;
  }
  @Override
  public BurstIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
    return getAssetTransferTable().getManyBy(ASSET_TRANSFER.ASSET_ID.eq(assetId), from, to);
  }

  @Override
  public BurstIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
    try ( DSLContext ctx = Db.getDSLContext() ) {

      return getAssetTransferTable().getManyBy(
        ctx,
        ctx
          .selectFrom(ASSET_TRANSFER).where(
            ASSET_TRANSFER.SENDER_ID.eq(accountId)
          )
          .unionAll(
            ctx.selectFrom(ASSET_TRANSFER).where(
              ASSET_TRANSFER.RECIPIENT_ID.eq(accountId).and(ASSET_TRANSFER.SENDER_ID.ne(accountId))
            )
          )
          .orderBy(ASSET_TRANSFER.HEIGHT.desc()).limit(from, to)
          .getQuery(),
        false
      );
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public BurstIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return getAssetTransferTable().getManyBy(
        ctx,
        ctx
          .selectFrom(ASSET_TRANSFER).where(
            ASSET_TRANSFER.SENDER_ID.eq(accountId).and(ASSET_TRANSFER.ASSET_ID.eq(assetId))
          )
          .unionAll(
            ctx.selectFrom(ASSET_TRANSFER).where(
              ASSET_TRANSFER.RECIPIENT_ID.eq(accountId)).and(
                ASSET_TRANSFER.SENDER_ID.ne(accountId)
              ).and(ASSET_TRANSFER.ASSET_ID.eq(assetId))
          )
          .orderBy(ASSET_TRANSFER.HEIGHT.desc()).limit(from, to)
          .getQuery(),
        false
      );
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public int getTransferCount(long assetId) {
    try (DSLContext ctx = Db.getDSLContext()) {
      return ctx.fetchCount(ctx.selectFrom(ASSET_TRANSFER).where(ASSET_TRANSFER.ASSET_ID.eq(assetId)));
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  protected class SqlAssetTransfer extends AssetTransfer {

    public SqlAssetTransfer(ResultSet rs) throws SQLException {
      super(rs.getLong("id"),
            transferDbKeyFactory.newKey(rs.getLong("id")),
            rs.getLong("asset_id"),
            rs.getInt("height"),
            rs.getLong("sender_id"),
            rs.getLong("recipient_id"),
            rs.getLong("quantity"),
            rs.getInt("timestamp")
            );
    }
  }


}
