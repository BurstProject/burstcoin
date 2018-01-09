package brs.services;

import brs.Alias;

public interface AliasService {

  Alias getAlias(long aliasId);

  Alias getAlias(String aliasName);
}
