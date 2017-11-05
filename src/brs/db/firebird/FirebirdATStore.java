package brs.db.firebird;

import brs.AT;
import brs.Burst;
import brs.at.AT_API_Helper;
import brs.db.sql.DbUtils;
import brs.db.sql.SqlATStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class FirebirdATStore extends SqlATStore {
  @Override
  protected void saveATState(Connection con, AT.ATState atState) throws SQLException {
    try (PreparedStatement pstmt = con.prepareStatement("UPDATE OR INSERT INTO at_state (at_id, "
                                                        + "state, prev_height ,next_height, sleep_between, prev_balance, freeze_when_same_balance, min_activate_amount, height, latest) "
                                                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) MATCHING(at_id, height)")) {
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
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }

  @Override
  protected void saveAT(Connection con, AT at) throws SQLException {
    try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO \"AT\" "
                                                        + "(id , creator_id , name , description , version , "
                                                        + "csize , dsize , c_user_stack_bytes , c_call_stack_bytes , "
                                                        + "creation_height , "
                                                        + "ap_code , height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      int i = 0;
      pstmt.setLong(++i, AT_API_Helper.getLong(at.getId()));
      pstmt.setLong(++i, AT_API_Helper.getLong(at.getCreator()));
      DbUtils.setString(pstmt, ++i, at.getName());
      DbUtils.setString(pstmt, ++i, at.getDescription());
      pstmt.setShort(++i, at.getVersion());
      pstmt.setInt(++i, at.getCsize());
      pstmt.setInt(++i, at.getDsize());
      pstmt.setInt(++i, at.getC_user_stack_bytes());
      pstmt.setInt(++i, at.getC_call_stack_bytes());
      pstmt.setInt(++i, at.getCreationBlockHeight());
      //DbUtils.setBytes( pstmt , ++i , this.getApCode() );
      DbUtils.setBytes(pstmt, ++i, AT.compressState(at.getApCode()));
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());

      pstmt.executeUpdate();
    }
  }
}
