package brs;

import brs.db.EntityTable;
import brs.db.NxtKey;

import java.util.Map;

public class Vote {

  private static final NxtKey.LongKeyFactory<Vote> voteDbKeyFactory = null;

  private static final EntityTable<Vote> voteTable = null;

  static Vote addVote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
    Vote vote = new Vote(transaction, attachment);
    voteTable.insert(vote);
    return vote;
  }

  public static int getCount() {
    return voteTable.getCount();
  }

  public static Vote getVote(long id) {
    return voteTable.get(voteDbKeyFactory.newKey(id));
  }

  public static Map<Long,Long> getVoters(Poll poll) {
    return Burst.getStores().getVoteStore().getVoters(poll);
  }

  static void init() {}


  private final long id;
  private final NxtKey dbKey;
  private final long pollId;
  private final long voterId;
  private final byte[] voteBytes;

  public Vote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
    this.id = transaction.getId();
    this.dbKey = voteDbKeyFactory.newKey(this.id);
    this.pollId = attachment.getPollId();
    this.voterId = transaction.getSenderId();
    this.voteBytes = attachment.getPollVote();
  }


  public long getId() {
    return id;
  }

  public long getPollId() { return pollId; }

  public long getVoterId() { return voterId; }

  public byte[] getVote() { return voteBytes; }

}
