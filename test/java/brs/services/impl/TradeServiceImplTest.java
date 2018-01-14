package brs.services.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Trade;
import brs.db.BurstIterator;
import brs.db.store.TradeStore;
import org.junit.Before;
import org.junit.Test;

public class TradeServiceImplTest {

  private TradeServiceImpl t;

  private TradeStore mockTradeStore;

  @Before
  public void setUp() {
    mockTradeStore = mock(TradeStore.class);

    t = new TradeServiceImpl(mockTradeStore);
  }

  @Test
  public void getAssetTrades() {
    final long assetId = 123l;
    final int from = 1;
    final int to = 5;

    final BurstIterator<Trade> mockTradesIterator = mock(BurstIterator.class);

    when(mockTradeStore.getAssetTrades(eq(assetId), eq(from), eq(to))).thenReturn(mockTradesIterator);

    assertEquals(mockTradesIterator, t.getAssetTrades(assetId, from, to));
  }
}
