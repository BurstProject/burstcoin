package nxt.db.store;

import nxt.Poll;

import java.util.Map;

public interface VoteStore {

    Map<Long,Long> getVoters(Poll poll);
}
