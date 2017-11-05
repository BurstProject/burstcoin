package brs.db.store;

import brs.Poll;
import java.util.Map;

public interface VoteStore {
  Map<Long,Long> getVoters(Poll poll);
}
