package nxt.db.store;

import nxt.Alias;
import nxt.db.NxtIterator;
import nxt.db.VersionedEntityTable;
import nxt.db.NxtKey;

public interface AliasStore {
    NxtKey.LongKeyFactory<Alias> getAliasDbKeyFactory();
    NxtKey.LongKeyFactory<Alias.Offer> getOfferDbKeyFactory();

    VersionedEntityTable<Alias> getAliasTable();

    VersionedEntityTable<Alias.Offer> getOfferTable();

    NxtIterator<Alias> getAliasesByOwner(long accountId, int from, int to);

    Alias getAlias(String aliasName);
}
