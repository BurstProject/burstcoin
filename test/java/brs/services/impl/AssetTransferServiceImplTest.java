package brs.services.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.AssetTransfer;
import brs.db.BurstIterator;
import brs.db.store.AssetTransferStore;
import org.junit.Before;
import org.junit.Test;

public class AssetTransferServiceImplTest {

  private AssetTransferServiceImpl t;

  private AssetTransferStore mockAssetTransferStore;

  @Before
  public void setUp() {
    mockAssetTransferStore = mock(AssetTransferStore.class);

    t = new AssetTransferServiceImpl(mockAssetTransferStore);
  }

  @Test
  public void getAssetTransfers() {
    final long assetId = 123L;
    final int from = 1;
    final int to = 4;

    final BurstIterator<AssetTransfer> mockAssetTransferIterator = mock(BurstIterator.class);

    when(mockAssetTransferStore.getAssetTransfers(eq(assetId), eq(from), eq(to))).thenReturn(mockAssetTransferIterator);

    assertEquals(mockAssetTransferIterator, t.getAssetTransfers(assetId, from, to));
  }

  @Test
  public void getAccountAssetTransfers() {
    final long accountId = 12L;
    final long assetId = 123L;
    final int from = 1;
    final int to = 4;

    final BurstIterator<AssetTransfer> mockAccountAssetTransferIterator = mock(BurstIterator.class);

    when(mockAssetTransferStore.getAccountAssetTransfers(eq(accountId), eq(assetId), eq(from), eq(to))).thenReturn(mockAccountAssetTransferIterator);

    assertEquals(mockAccountAssetTransferIterator, t.getAccountAssetTransfers(accountId, assetId, from, to));
  }
}

