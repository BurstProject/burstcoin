package brs.services.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.DigitalGoodsStore;
import brs.db.BurstIterator;
import brs.db.VersionedEntityTable;
import brs.db.store.DigitalGoodsStoreStore;
import brs.schema.tables.Goods;
import org.junit.Before;
import org.junit.Test;

public class DGSGoodsStoreServiceImplTest {

  private DGSGoodsStoreServiceImpl t;

  private DigitalGoodsStoreStore mockDigitalGoodsStoreStore;
  private VersionedEntityTable<DigitalGoodsStore.Goods> mockGoodsTable;

  @Before
  public void setUp() {
    mockGoodsTable = mock(VersionedEntityTable.class);
    mockDigitalGoodsStoreStore = mock(DigitalGoodsStoreStore.class);
    when(mockDigitalGoodsStoreStore.getGoodsTable()).thenReturn(mockGoodsTable);

    t = new DGSGoodsStoreServiceImpl(mockDigitalGoodsStoreStore);
  }


  @Test
  public void getAllGoods() {
    final int from = 1;
    final int to = 2;

    final BurstIterator<DigitalGoodsStore.Goods> mockIterator = mock(BurstIterator.class);
    when(mockGoodsTable.getAll(eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getAllGoods(from, to));
  }

  @Test
  public void getGoodsInStock() {
    final int from = 1;
    final int to = 2;

    final BurstIterator<DigitalGoodsStore.Goods> mockIterator = mock(BurstIterator.class);
    when(mockDigitalGoodsStoreStore.getGoodsInStock(eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getGoodsInStock(from, to));
  }

  @Test
  public void getSellerGoods() {
    final long sellerId = 1L;
    final boolean inStockOnly = false;
    final int from = 1;
    final int to = 2;

    final BurstIterator<DigitalGoodsStore.Goods> mockIterator = mock(BurstIterator.class);
    when(mockDigitalGoodsStoreStore.getSellerGoods(eq(sellerId), eq(inStockOnly), eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getSellerGoods(sellerId, inStockOnly, from, to));
  }
}