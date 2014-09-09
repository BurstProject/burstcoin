package nxt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Alias {

    public static class Offer {

        private final long priceNQT;
        private final Long buyerId;

        private Offer(long priceNQT, Long buyerId) {
            this.priceNQT = priceNQT;
            this.buyerId = buyerId;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public Long getBuyerId() {
            return buyerId;
        }

    }

    private static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap<>();
    private static final Collection<Alias> allAliases = Collections.unmodifiableCollection(aliases.values());
    private static final ConcurrentMap<String, Offer> aliasesToSell = new ConcurrentHashMap<>();

    public static Collection<Alias> getAllAliases() {
        return allAliases;
    }

    public static Collection<Alias> getAliasesByOwner(Long accountId) {
        List<Alias> filtered = new ArrayList<>();
        for (Alias alias : Alias.getAllAliases()) {
            if (alias.getAccountId().equals(accountId)) {
                filtered.add(alias);
            }
        }
        return filtered;
    }

    public static Alias getAlias(String aliasName) {
        return aliases.get(aliasName.toLowerCase());
    }

    public static Alias getAlias(Long id) {
        return aliasIdToAliasMappings.get(id);
    }

    public static Offer getOffer(String aliasName) {
        return aliasesToSell.get(aliasName.toLowerCase());
    }

    static void addOrUpdateAlias(Account account, Long transactionId, String aliasName, String aliasURI, int timestamp) {
        String normalizedAlias = aliasName.toLowerCase();
        Alias oldAlias = aliases.get(normalizedAlias);
        if (oldAlias == null) {
            Alias newAlias = new Alias(account, transactionId, aliasName, aliasURI, timestamp);
            aliases.put(normalizedAlias, newAlias);
            aliasIdToAliasMappings.put(transactionId, newAlias);
        } else {
            oldAlias.aliasURI = aliasURI.intern();
            oldAlias.timestamp = timestamp;
        }
    }

    static void remove(Alias alias) {
        aliases.remove(alias.getAliasName().toLowerCase());
        aliasIdToAliasMappings.remove(alias.getId());
    }

    static void addSellOffer(String aliasName, long priceNQT, Account buyerAccount) {
        aliasesToSell.put(aliasName.toLowerCase(), new Offer(priceNQT, buyerAccount != null ? buyerAccount.getId() : null));
    }

    static void changeOwner(Account newOwner, String aliasName, int timestamp) {
        String normalizedName = aliasName.toLowerCase();
        Alias oldAlias = aliases.get(normalizedName);
        Long id = oldAlias.getId();
        Alias newAlias = new Alias(newOwner, id, aliasName, oldAlias.aliasURI, timestamp);
        aliasesToSell.remove(normalizedName);
        aliases.put(normalizedName, newAlias);
        aliasIdToAliasMappings.put(id, newAlias);
    }

    static void clear() {
        aliases.clear();
        aliasIdToAliasMappings.clear();
        aliasesToSell.clear();
    }

    private final Long accountId;
    private final Long id;
    private final String aliasName;
    private volatile String aliasURI;
    private volatile int timestamp;

    private Alias(Account account, Long id, String aliasName, String aliasURI, int timestamp) {
        this.accountId = account.getId();
        this.id = id;
        this.aliasName = aliasName.intern();
        this.aliasURI = aliasURI.intern();
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getAliasURI() {
        return aliasURI;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Long getAccountId() {
        return accountId;
    }

}
