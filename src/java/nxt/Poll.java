package nxt;



import nxt.db.EntityTable;
import nxt.db.NxtIterator;
import nxt.db.NxtKey;

import java.util.Map;

public final class Poll {

    private static final NxtKey.LongKeyFactory<Poll> pollDbKeyFactory = null;

    private static final EntityTable<Poll> pollTable = null;

    static void init() {}


    private final long id;
    private final NxtKey dbKey;
    private final String name;
    private final String description;
    private final String[] options;
    private final byte minNumberOfOptions, maxNumberOfOptions;
    private final boolean optionsAreBinary;

    private Poll(long id, Attachment.MessagingPollCreation attachment) {
        this.id = id;
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = attachment.getPollName();
        this.description = attachment.getPollDescription();
        this.options = attachment.getPollOptions();
        this.minNumberOfOptions = attachment.getMinNumberOfOptions();
        this.maxNumberOfOptions = attachment.getMaxNumberOfOptions();
        this.optionsAreBinary = attachment.isOptionsAreBinary();
    }



    static void addPoll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        pollTable.insert(new Poll(transaction.getId(), attachment));
    }

    public static Poll getPoll(long id) {
        return pollTable.get(pollDbKeyFactory.newKey(id));
    }

    public static NxtIterator<Poll> getAllPolls(int from, int to) {
        return pollTable.getAll(from, to);
    }

    public static int getCount() {
        return pollTable.getCount();
    }


    public long getId() {
        return id;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String[] getOptions() { return options; }

    public byte getMinNumberOfOptions() { return minNumberOfOptions; }

    public byte getMaxNumberOfOptions() { return maxNumberOfOptions; }

    public boolean isOptionsAreBinary() { return optionsAreBinary; }

    public Map<Long, Long> getVoters() {
        return Vote.getVoters(this);
    }

}
