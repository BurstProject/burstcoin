package nxt;

import nxt.db.NxtKey;
import nxt.db.VersionedEntityTable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Hub {

    public static class Hit implements Comparable<Hit> {

        public final Hub hub;
        public final long hitTime;

        private Hit(Hub hub, long hitTime) {
            this.hub = hub;
            this.hitTime = hitTime;
        }

        @Override
        public int compareTo(Hit hit) {
            if (this.hitTime < hit.hitTime) {
                return -1;
            } else if (this.hitTime > hit.hitTime) {
                return 1;
            } else {
                return Long.compare(this.hub.accountId, hit.hub.accountId);
            }
        }

    }

    private static final NxtKey.LongKeyFactory<Hub> hubDbKeyFactory = null;

    private static final VersionedEntityTable<Hub> hubTable = null;

    static void addOrUpdateHub(Transaction transaction, Attachment.MessagingHubAnnouncement attachment) {
        hubTable.insert(new Hub(transaction, attachment));
    }

    private static long lastBlockId;
    private static List<Hit> lastHits;

    public static List<Hit> getHubHits(Block block) {

        /*synchronized (Hub.class) {
            if (block.getId() == lastBlockId && lastHits != null) {
                return lastHits;
            }
            List<Hit> currentHits = new ArrayList<>();
            long currentLastBlockId;

            synchronized (BlockchainImpl.getInstance()) {
                currentLastBlockId = BlockchainImpl.getInstance().getLastBlock().getId();
                if (currentLastBlockId != block.getId()) {
                    return Collections.emptyList();
                }
                try (DbIterator<Hub> hubs = hubTable.getAll(0, -1)) {
                    while (hubs.hasNext()) {
                        Hub hub = hubs.next();
                        Account account = Account.getAccount(hub.getAccountId());
                        if (account != null && account.getEffectiveBalanceNXT() >= Constants.MIN_HUB_EFFECTIVE_BALANCE
                                && account.getPublicKey() != null) {
                            currentHits.add(new Hit(hub, Generator.getHitTime(account, block)));
                        }
                    }
                }
            }

            Collections.sort(currentHits);
            lastHits = currentHits;
            lastBlockId = currentLastBlockId;
        }
        return lastHits;*/
    	return null;
    }

    static void init() {}


    private final long accountId;
    private final NxtKey dbKey;
    private final long minFeePerByteNQT;
    private final List<String> uris;

    private Hub(Transaction transaction, Attachment.MessagingHubAnnouncement attachment) {
        this.accountId = transaction.getSenderId();
        this.dbKey = hubDbKeyFactory.newKey(this.accountId);
        this.minFeePerByteNQT = attachment.getMinFeePerByteNQT();
        this.uris = Collections.unmodifiableList(Arrays.asList(attachment.getUris()));
    }
//

    public long getAccountId() {
        return accountId;
    }

    public long getMinFeePerByteNQT() {
        return minFeePerByteNQT;
    }

    public List<String> getUris() {
        return uris;
    }

}
