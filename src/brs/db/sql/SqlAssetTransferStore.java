package brs.db.sql;

import brs.AssetTransfer;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.store.AssetTransferStore;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static brs.schema.Tables.ASSET_TRANSFER;

public abstract class SqlAssetTransferStore implements AssetTransferStore {

  protected static final BurstKey.LongKeyFactory<AssetTransfer> transferDbKeyFactory = new DbKey.LongKeyFactory<AssetTransfer>("id") {

      @Override
      public BurstKey newKey(AssetTransfer assetTransfer) {
        return assetTransfer.dbKey;
      }
    };
  private final EntitySqlTable<AssetTransfer> assetTransferTable = new EntitySqlTable<AssetTransfer>("asset_transfer", brs.schema.Tables.ASSET_TRANSFER, transferDbKeyFactory) {

      @Override
      protected AssetTransfer load(Connection con, ResultSet rs) throws SQLException {
        return new SqlAssetTransfer(rs);
      }

      @Override
      protected void save(Connection con, AssetTransfer assetTransfer) throws SQLException {
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
    return getAssetTransferTable().getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
  }

  @Override
  public BurstIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
    try (Connection con = Db.getConnection();
         PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ?"
                                                        + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? ORDER BY height DESC"
                                                        + DbUtils.limitsClause(from, to))) {
      int i = 0;
      pstmt.setLong(++i, accountId);
      pstmt.setLong(++i, accountId);
      pstmt.setLong(++i, accountId);
      DbUtils.setLimits(++i, pstmt, from, to);
      return getAssetTransferTable().getManyBy(con, pstmt, false);
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public BurstIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
    try (Connection con = Db.getConnection();
         PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ? AND asset_id = ?"
                                                        + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? AND asset_id = ? ORDER BY height DESC"
                                                        + DbUtils.limitsClause(from, to))) {
      int i = 0;
      pstmt.setLong(++i, accountId);
      pstmt.setLong(++i, assetId);
      pstmt.setLong(++i, accountId);
      pstmt.setLong(++i, accountId);
      pstmt.setLong(++i, assetId);
      DbUtils.setLimits(++i, pstmt, from, to);
      return getAssetTransferTable().getManyBy(con, pstmt, false);
    } catch (SQLException e) {
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
