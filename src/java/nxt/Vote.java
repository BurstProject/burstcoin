package nxt;

import nxt.util.Convert;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Vote {

    private static final ConcurrentMap<Long, Vote> votes = new ConcurrentHashMap<>();

    private final Long id;
    private final Long pollId;
    private final Long voterId;
    private final byte[] vote;

    private Vote(Long id, Long pollId, Long voterId, byte[] vote) {

        this.id = id;
        this.pollId = pollId;
        this.voterId = voterId;
        this.vote = vote;

    }

    static Vote addVote(Long id, Long pollId, Long voterId, byte[] vote) {
        Vote voteData = new Vote(id, pollId, voterId, vote);
        if (votes.putIfAbsent(id, voteData) != null) {
            throw new IllegalStateException("Vote with id " + Convert.toUnsignedLong(id) + " already exists");
        }
        return voteData;
    }

    public static Map<Long, Vote> getVotes() {
        return Collections.unmodifiableMap(votes);
    }

    static void clear() {
        votes.clear();
    }

    public static Vote getVote(Long id) {
        return votes.get(id);
    }

    public Long getId() {
        return id;
    }

    public Long getPollId() { return pollId; }

    public Long getVoterId() { return voterId; }

    public byte[] getVote() { return vote; }

}
