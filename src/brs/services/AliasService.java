package brs.services;

import brs.Alias;
import brs.Alias.Offer;
import brs.db.BurstIterator;

public interface AliasService {

  Alias getAlias(long aliasId);

  Alias getAlias(String aliasName);

  Offer getOffer(Alias alias);

  long getAliasCount();

  BurstIterator<Alias> getAliasesByOwner(long accountId, int from, int to);

}
