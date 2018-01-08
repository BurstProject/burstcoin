package brs.services.impl;

import brs.Alias;
import brs.Burst;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.services.AliasService;

public class AliasServiceImpl implements AliasService {


  private final VersionedEntityTable<Alias> aliasTable;
  private final BurstKey.LongKeyFactory<Alias> aliasDbKeyFactory;

  public AliasServiceImpl(VersionedEntityTable<Alias> aliasTable, BurstKey.LongKeyFactory<Alias> aliasDbKeyFactory) {
    this.aliasTable = aliasTable;
    this.aliasDbKeyFactory = aliasDbKeyFactory;
  }

  public Alias getAlias(String aliasName) {
    return Burst.getStores().getAliasStore().getAlias(aliasName);
  }

  public Alias getAlias(long id) {
    return aliasTable.get(aliasDbKeyFactory.newKey(id));
  }

}
