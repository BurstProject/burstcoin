package brs.services.impl;

import brs.Asset;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.sql.EntitySqlTable;
import brs.services.AssetService;

public class AssetServiceImpl implements AssetService {

  private final EntitySqlTable<Asset> assetTable;

  private final BurstKey.LongKeyFactory<Asset> assetDbKeyFactory;

  public AssetServiceImpl(EntitySqlTable<Asset> assetTable, LongKeyFactory<Asset> assetDbKeyFactory) {
    this.assetDbKeyFactory = assetDbKeyFactory;
    this.assetTable = assetTable;
  }

  @Override
  public Asset getAsset(long id) {
    return assetTable.get(assetDbKeyFactory.newKey(id));
  }

}
