package brs.services.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.sql.EntitySqlTable;
import brs.db.store.AssetStore;
import org.junit.Before;
import org.junit.Test;

public class AssetServiceImplTest {

  private AssetServiceImpl t;


  private AssetStore mockAssetStore;
  private EntitySqlTable assetTableMock;
  private LongKeyFactory assetDbKeyFactoryMock;

  @Before
  public void setUp() {
    mockAssetStore = mock(AssetStore.class);
    assetTableMock = mock(EntitySqlTable.class);
    assetDbKeyFactoryMock = mock(LongKeyFactory.class);

    when(mockAssetStore.getAssetTable()).thenReturn(assetTableMock);
    when(mockAssetStore.getAssetDbKeyFactory()).thenReturn(assetDbKeyFactoryMock);

    t = new AssetServiceImpl(mockAssetStore);
  }

  @Test
  public void getAsset() {
    final long assetId = 123l;
    final Asset mockAsset = mock(Asset.class);
    final BurstKey assetKeyMock = mock(BurstKey.class);

    when(assetDbKeyFactoryMock.newKey(eq(assetId))).thenReturn(assetKeyMock);
    when(assetTableMock.get(eq(assetKeyMock))).thenReturn(mockAsset);

    assertEquals(mockAsset, t.getAsset(assetId));
  }
}