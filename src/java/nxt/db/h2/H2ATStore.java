package nxt.db.h2;

import nxt.AT;
import nxt.Nxt;
import nxt.db.sql.DbUtils;
import nxt.db.sql.SqlATStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class H2ATStore extends SqlATStore {
    @Override
    protected void saveATState(Connection con, AT.ATState atState) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE  INTO at_state (at_id, "
                + "state, prev_height ,next_height, sleep_between, prev_balance, freeze_when_same_balance, min_activate_amount, height, latest) "
                + "KEY (at_id, height)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, atState.getATId());
            //DbUtils.setBytes(pstmt, ++i, state);
            DbUtils.setBytes(pstmt, ++i, AT.compressState(atState.getState()));
            pstmt.setInt(++i, atState.getPrevHeight());
            pstmt.setInt(++i, atState.getNextHeight());
            pstmt.setInt(++i, atState.getSleepBetween());
            pstmt.setLong(++i, atState.getPrevBalance());
            pstmt.setBoolean(++i, atState.getFreezeWhenSameBalance());
            pstmt.setLong(++i, atState.getMinActivationAmount());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
}
