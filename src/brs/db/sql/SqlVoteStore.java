package brs.db.sql;

import brs.Attachment;
import brs.Poll;
import brs.Transaction;
import brs.Vote;
import brs.db.EntityTable;
import brs.db.BurstIterator;
import brs.db.store.VoteStore;

import java.util.HashMap;
import java.util.Map;

public abstract class SqlVoteStore implements VoteStore {

  private static final EntityTable<Vote> voteTable = null;

  @Override
  public Map<Long, Long> getVoters(Poll poll) {
    Map<Long, Long> map = new HashMap<>();
    try (BurstIterator<Vote> voteIterator = voteTable.getManyBy(new DbClause.LongClause("poll_id", poll.getId()), 0, -1)) {
      while (voteIterator.hasNext()) {
        Vote vote = voteIterator.next();
        map.put(vote.getVoterId(), vote.getId());
      }
    }
    return map;
  }

  private class SqlVote extends Vote {
    private SqlVote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
      super(transaction, attachment);
    }

    //        private SqlVote(ResultSet rs) throws SQLException {
    //            this.id = rs.getLong("id");
    //            this.dbKey = (DbKey)voteDbKeyFactory.newKey(this.id);
    //            this.pollId = rs.getLong("poll_id");
    //            this.voterId = rs.getLong("voter_id");
    //            this.voteBytes = rs.getBytes("vote_bytes");
    //        }
    //
    //        private void save(Connection con) throws SQLException {
    //            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote (id, poll_id, voter_id, "
    //                    + "vote_bytes, height) VALUES (?, ?, ?, ?, ?)")) {
    //                int i = 0;
    //                pstmt.setLong(++i, this.getId());
    //                pstmt.setLong(++i, this.getPollId());
    //                pstmt.setLong(++i, this.getVoterId());
    //                pstmt.setBytes(++i, this.getVote());
    //                pstmt.setInt(++i, Burst.getBlockchain().getHeight());
    //                pstmt.executeUpdate();
    //            }
    //        }
  }

}
