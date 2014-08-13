package nxt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
                return this.hub.accountId.compareTo(hit.hub.accountId);
            }
        }

    }

    private static final ConcurrentMap<Long, Hub> hubs = new ConcurrentHashMap<>();

    static void addOrUpdateHub(Long accountId, long minFeePerByteNQT, String[] uris) {
        hubs.put(accountId, new Hub(accountId, minFeePerByteNQT, uris));
    }

    static void removeHub(Long accountId) {
        hubs.remove(accountId);
    }

    private static Long lastBlockId;
    private static List<Hit> lastHits;

    public static List<Hit> getHubHits(Block block) {

        /*synchronized (Hub.class) {
            if (block.getId().equals(lastBlockId) && lastHits != null) {
                return lastHits;
            }
            List<Hit> currentHits = new ArrayList<>();
            Long currentLastBlockId;

            synchronized (BlockchainImpl.getInstance()) {
                currentLastBlockId = BlockchainImpl.getInstance().getLastBlock().getId();
                if (! currentLastBlockId.equals(block.getId())) {
                    return Collections.emptyList();
                }
                for (Map.Entry<Long, Hub> hubEntry : hubs.entrySet()) {
                    Account account = Account.getAccount(hubEntry.getKey());
                    if (account != null && account.getEffectiveBalanceNXT() >= Constants.MIN_HUB_EFFECTIVE_BALANCE
                            && account.getPublicKey() != null) {
                        currentHits.add(new Hit(hubEntry.getValue(), Generator.getHitTime(account, block)));
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

    private final Long accountId;
    private final long minFeePerByteNQT;
    private final List<String> uris;

    private Hub(Long accountId, long minFeePerByteNQT, String[] uris) {
        this.accountId = accountId;
        this.minFeePerByteNQT = minFeePerByteNQT;
        this.uris = Collections.unmodifiableList(Arrays.asList(uris));
    }

    public Long getAccountId() {
        return accountId;
    }

    public long getMinFeePerByteNQT() {
        return minFeePerByteNQT;
    }

    public List<String> getUris() {
        return uris;
    }

}
