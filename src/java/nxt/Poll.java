package nxt;

import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public final class Poll {

    private static final DbKey.LongKeyFactory<Poll> pollDbKeyFactory = null;

    private static final EntityDbTable<Poll> pollTable = null;

    static void init() {}


    private final long id;
    private final DbKey dbKey;
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

    private Poll(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = pollDbKeyFactory.newKey(this.id);
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.options = (String[])rs.getArray("options").getArray();
        this.minNumberOfOptions = rs.getByte("min_num_options");
        this.maxNumberOfOptions = rs.getByte("max_num_options");
        this.optionsAreBinary = rs.getBoolean("binary_options");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, name, description, "
                + "options, min_num_options, max_num_options, binary_options, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setString(++i, this.getName());
            pstmt.setString(++i, this.getDescription());
            pstmt.setObject(++i, this.getOptions());
            pstmt.setByte(++i, this.getMinNumberOfOptions());
            pstmt.setByte(++i, this.getMaxNumberOfOptions());
            pstmt.setBoolean(++i, this.isOptionsAreBinary());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    static void addPoll(Transaction transaction, Attachment.MessagingPollCreation attachment) {
        pollTable.insert(new Poll(transaction.getId(), attachment));
    }

    public static Poll getPoll(long id) {
        return pollTable.get(pollDbKeyFactory.newKey(id));
    }

    public static DbIterator<Poll> getAllPolls(int from, int to) {
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
