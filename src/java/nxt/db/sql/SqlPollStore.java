package nxt.db.sql;

import nxt.Nxt;
import nxt.Poll;
import nxt.db.store.PollStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SqlPollStore implements PollStore {

//    protected  class SqlPoll extends Poll
//    {
////        private SqlPoll(ResultSet rs) throws SQLException {
////            this.id = rs.getLong("id");
////            this.dbKey =(DbKey) pollDbKeyFactory.newKey(this.id);
////            this.name = rs.getString("name");
////            this.description = rs.getString("description");
////            this.options = (String[])rs.getArray("options").getArray();
////            this.minNumberOfOptions = rs.getByte("min_num_options");
////            this.maxNumberOfOptions = rs.getByte("max_num_options");
////            this.optionsAreBinary = rs.getBoolean("binary_options");
////        }
////
////        private void save(Connection con) throws SQLException {
////            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, name, description, "
////                    + "options, min_num_options, max_num_options, binary_options, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
////                int i = 0;
////                pstmt.setLong(++i, this.getId());
////                pstmt.setString(++i, this.getName());
////                pstmt.setString(++i, this.getDescription());
////                pstmt.setObject(++i, this.getOptions());
////                pstmt.setByte(++i, this.getMinNumberOfOptions());
////                pstmt.setByte(++i, this.getMaxNumberOfOptions());
////                pstmt.setBoolean(++i, this.isOptionsAreBinary());
////                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
////                pstmt.executeUpdate();
////            }
////        }
//    }
}
