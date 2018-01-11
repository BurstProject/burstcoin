package brs.services;

import brs.Alias;
import brs.Alias.Offer;

public interface AliasService {

  Alias getAlias(long aliasId);

  Alias getAlias(String aliasName);

  Offer getOffer(Alias alias);
}
