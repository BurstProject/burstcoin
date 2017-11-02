package brs.db.sql;

import brs.Asset;
import brs.Attachment;
import brs.Nxt;
import brs.Transaction;
import brs.db.NxtIterator;
import brs.db.NxtKey;
import brs.db.store.AssetStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract  class SqlAssetStore implements AssetStore {

    private final NxtKey.LongKeyFactory<Asset> assetDbKeyFactory = new DbKey.LongKeyFactory<Asset>("id") {

        @Override
        public NxtKey newKey(Asset asset) {
            return asset.dbKey;
        }

    };
    private final EntitySqlTable<Asset> assetTable = new EntitySqlTable<Asset>("asset", assetDbKeyFactory) {

        @Override
        protected Asset load(Connection con, ResultSet rs) throws SQLException {
            return new SqlAsset(rs);
        }

        @Override
        protected void save(Connection con, Asset asset) throws SQLException {
            saveAsset(con, asset);
        }
    };

    private void saveAsset(Connection con, Asset asset) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset (id, account_id, name, "
                + "description, quantity, decimals, height) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, asset.getId());
            pstmt.setLong(++i, asset.getAccountId());
            pstmt.setString(++i, asset.getName());
            pstmt.setString(++i, asset.getDescription());
            pstmt.setLong(++i, asset.getQuantityQNT());
            pstmt.setByte(++i, asset.getDecimals());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public NxtKey.LongKeyFactory<Asset> getAssetDbKeyFactory() {
        return assetDbKeyFactory;
    }

    @Override
    public EntitySqlTable<Asset> getAssetTable() {
        return assetTable;
    }

    @Override
    public NxtIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
        return assetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    private class SqlAsset extends Asset {

        private SqlAsset(ResultSet rs) throws SQLException {
            super(rs.getLong("id"),
                    assetDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getLong("account_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getLong("quantity"),
                    rs.getByte("decimals")
            );
        }
    }
}
