package brs.db.store;

import brs.Alias;
import brs.db.NxtIterator;
import brs.db.VersionedEntityTable;
import brs.db.NxtKey;

public interface AliasStore {
    NxtKey.LongKeyFactory<Alias> getAliasDbKeyFactory();
    NxtKey.LongKeyFactory<Alias.Offer> getOfferDbKeyFactory();

    VersionedEntityTable<Alias> getAliasTable();

    VersionedEntityTable<Alias.Offer> getOfferTable();

    NxtIterator<Alias> getAliasesByOwner(long accountId, int from, int to);

    Alias getAlias(String aliasName);
}
