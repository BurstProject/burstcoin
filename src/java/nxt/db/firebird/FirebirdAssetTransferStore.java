package nxt.db.firebird;

import nxt.AssetTransfer;
import nxt.db.sql.EntitySqlTable;
import nxt.db.sql.SqlAssetTransferStore;
import nxt.db.store.AssetTransferStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class FirebirdAssetTransferStore extends SqlAssetTransferStore {
    private final EntitySqlTable<AssetTransfer> assetTransferTable = new EntitySqlTable<AssetTransfer>("asset_transfer", transferDbKeyFactory) {

        @Override
        protected AssetTransfer load(Connection con, ResultSet rs) throws SQLException {
            return new SqlAssetTransfer(rs);
        }

        @Override
        protected void save(Connection con, AssetTransfer assetTransfer) throws SQLException {
            saveAssetTransfer(con, assetTransfer);
        }
    };

    private void saveAssetTransfer(Connection con, AssetTransfer assetTransfer) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_transfer (id, asset_id, "
                + "sender_id, recipient_id, quantity, \"timestamp\", height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, assetTransfer.getId());
            pstmt.setLong(++i, assetTransfer.getAssetId());
            pstmt.setLong(++i, assetTransfer.getSenderId());
            pstmt.setLong(++i, assetTransfer.getRecipientId());
            pstmt.setLong(++i, assetTransfer.getQuantityQNT());
            pstmt.setInt(++i, assetTransfer.getTimestamp());
            pstmt.setInt(++i, assetTransfer.getHeight());
            pstmt.executeUpdate();
        }
    }


    @Override
    public EntitySqlTable<AssetTransfer> getAssetTransferTable() {
        return assetTransferTable;
    }

}
