package brs.services.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account.AccountAsset;
import brs.Asset;
import brs.AssetTransfer;
import brs.Burst;
import brs.Trade;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.sql.EntitySqlTable;
import brs.db.store.AssetStore;
import brs.services.AssetAccountService;
import brs.services.AssetTransferService;
import brs.services.TradeService;
import org.junit.Before;
import org.junit.Test;

public class AssetServiceImplTest {

  private AssetServiceImpl t;

  private AssetAccountService mockAssetAccountService;
  private AssetTransferService mockAssetTransferService;
  private TradeService mockTradeService;
  private AssetStore mockAssetStore;
  private EntitySqlTable mockAssetTableMock;
  private LongKeyFactory mockAssetDbKeyFactoryMock;

  @Before
  public void setUp() {
    mockAssetAccountService = mock(AssetAccountService.class);
    mockAssetTransferService = mock(AssetTransferService.class);
    mockTradeService = mock(TradeService.class);

    mockAssetStore = mock(AssetStore.class);
    mockAssetTableMock = mock(EntitySqlTable.class);
    mockAssetDbKeyFactoryMock = mock(LongKeyFactory.class);

    when(mockAssetStore.getAssetTable()).thenReturn(mockAssetTableMock);
    when(mockAssetStore.getAssetDbKeyFactory()).thenReturn(mockAssetDbKeyFactoryMock);

    t = new AssetServiceImpl(mockAssetAccountService, mockTradeService, mockAssetStore, mockAssetTransferService);
  }

  @Test
  public void getAsset() {
    final long assetId = 123l;
    final Asset mockAsset = mock(Asset.class);
    final BurstKey assetKeyMock = mock(BurstKey.class);

    when(mockAssetDbKeyFactoryMock.newKey(eq(assetId))).thenReturn(assetKeyMock);
    when(mockAssetTableMock.get(eq(assetKeyMock))).thenReturn(mockAsset);

    assertEquals(mockAsset, t.getAsset(assetId));
  }

  @Test
  public void getAccounts() {
    final long assetId = 123l;
    final int from = 1;
    final int to = 5;

    final BurstIterator<AccountAsset> mockAccountAssetIterator = mock(BurstIterator.class);

    when(mockAssetAccountService.getAssetAccounts(eq(assetId), eq(from), eq(to))).thenReturn(mockAccountAssetIterator);

    assertEquals(mockAccountAssetIterator, t.getAccounts(assetId, from, to));
  }

  @Test
  public void getAccounts_forHeight() {
    final long assetId = 123l;
    final int from = 1;
    final int to = 5;
    final int height = 3;

    final BurstIterator<AccountAsset> mockAccountAssetIterator = mock(BurstIterator.class);

    when(mockAssetAccountService.getAssetAccounts(eq(assetId), eq(height), eq(from), eq(to))).thenReturn(mockAccountAssetIterator);

    assertEquals(mockAccountAssetIterator, t.getAccounts(assetId, height, from, to));
  }

  @Test
  public void getAccounts_forHeight_negativeHeightGivesForZeroHeight() {
    final long assetId = 123l;
    final int from = 1;
    final int to = 5;
    final int height = -3;

    final BurstIterator<AccountAsset> mockAccountAssetIterator = mock(BurstIterator.class);

    when(mockAssetAccountService.getAssetAccounts(eq(assetId), eq(from), eq(to))).thenReturn(mockAccountAssetIterator);

    assertEquals(mockAccountAssetIterator, t.getAccounts(assetId, height, from, to));
  }

  @Test
  public void getTrades() {
    final long assetId = 123l;
    final int from = 2;
    final int to = 4;

    final BurstIterator<Trade> mockTradeIterator = mock(BurstIterator.class);

    when(mockTradeService.getAssetTrades(eq(assetId), eq(from), eq(to))).thenReturn(mockTradeIterator);

    assertEquals(mockTradeIterator, t.getTrades(assetId, from, to));
  }

  @Test
  public void getAssetTransfers() {
    final long assetId = 123l;
    final int from = 2;
    final int to = 4;

    final BurstIterator<AssetTransfer> mockTransferIterator = mock(BurstIterator.class);

    when(mockAssetTransferService.getAssetTransfers(eq(assetId), eq(from), eq(to))).thenReturn(mockTransferIterator);

    assertEquals(mockTransferIterator, t.getAssetTransfers(assetId, from, to));
  }
}