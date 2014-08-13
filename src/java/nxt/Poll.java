package nxt;

import nxt.util.Convert;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Poll {

    private static final ConcurrentMap<Long, Poll> polls = new ConcurrentHashMap<>();
    private static final Collection<Poll> allPolls = Collections.unmodifiableCollection(polls.values());

    private final Long id;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte minNumberOfOptions, maxNumberOfOptions;
    private final boolean optionsAreBinary;
    private final ConcurrentMap<Long, Long> voters;

    private Poll(Long id, String name, String description, String[] options, byte minNumberOfOptions, byte maxNumberOfOptions, boolean optionsAreBinary) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.options = options;
        this.minNumberOfOptions = minNumberOfOptions;
        this.maxNumberOfOptions = maxNumberOfOptions;
        this.optionsAreBinary = optionsAreBinary;
        this.voters = new ConcurrentHashMap<>();

    }

    static void addPoll(Long id, String name, String description, String[] options, byte minNumberOfOptions, byte maxNumberOfOptions, boolean optionsAreBinary) {
        if (polls.putIfAbsent(id, new Poll(id, name, description, options, minNumberOfOptions, maxNumberOfOptions, optionsAreBinary)) != null) {
            throw new IllegalStateException("Poll with id " + Convert.toUnsignedLong(id) + " already exists");
        }
    }

    public static Collection<Poll> getAllPolls() {
        return allPolls;
    }

    static void clear() {
        polls.clear();
    }

    public static Poll getPoll(Long id) {
        return polls.get(id);
    }

    public Long getId() {
        return id;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String[] getOptions() { return options; }

    public byte getMinNumberOfOptions() { return minNumberOfOptions; }

    public byte getMaxNumberOfOptions() { return maxNumberOfOptions; }

    public boolean isOptionsAreBinary() { return optionsAreBinary; }

    public Map<Long, Long> getVoters() {
        return Collections.unmodifiableMap(voters);
    }

    void addVoter(Long voterId, Long voteId) {
        voters.put(voterId, voteId);
    }

}
