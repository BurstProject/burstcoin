package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Alias {

    private static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap<>();
    private static final Collection<Alias> allAliases = Collections.unmodifiableCollection(aliases.values());

    public static Collection<Alias> getAllAliases() {
        return allAliases;
    }

    public static Alias getAlias(String aliasName) {
        return aliases.get(aliasName.toLowerCase());
    }

    public static Alias getAlias(Long id) {
        return aliasIdToAliasMappings.get(id);
    }

    static void addOrUpdateAlias(Account account, Long transactionId, String aliasName, String aliasURI, int timestamp) {
        String normalizedAlias = aliasName.toLowerCase();
        Alias newAlias = new Alias(account, transactionId, aliasName, aliasURI, timestamp);
        Alias oldAlias = aliases.putIfAbsent(normalizedAlias, newAlias);
        if (oldAlias == null) {
            aliasIdToAliasMappings.putIfAbsent(transactionId, newAlias);
        } else {
            oldAlias.aliasURI = aliasURI.intern();
            oldAlias.timestamp = timestamp;
        }
    }

    static void remove(Alias alias) {
        aliases.remove(alias.getAliasName().toLowerCase());
        aliasIdToAliasMappings.remove(alias.getId());
    }

    static void clear() {
        aliases.clear();
        aliasIdToAliasMappings.clear();
    }

    private final Account account;
    private final Long id;
    private final String aliasName;
    private volatile String aliasURI;
    private volatile int timestamp;

    private Alias(Account account, Long id, String aliasName, String aliasURI, int timestamp) {

        this.account = account;
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

    public Account getAccount() {
        return account;
    }

}
