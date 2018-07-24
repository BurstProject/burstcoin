package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.IN_STOCK_ONLY_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.SELLER_PARAMETER;
import static brs.http.common.ResultFields.DELISTED_RESPONSE;
import static brs.http.common.ResultFields.DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.GOODS_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.PRICE_NQT_RESPONSE;
import static brs.http.common.ResultFields.QUANTITY_RESPONSE;
import static brs.http.common.ResultFields.TAGS_RESPONSE;
import static brs.http.common.ResultFields.TIMESTAMP_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.BurstException;
import brs.DigitalGoodsStore;
import brs.DigitalGoodsStore.Goods;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.db.sql.DbUtils;
import brs.services.DGSGoodsStoreService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@SuppressStaticInitializationFor("brs.db.sql.DbUtils")
@PrepareForTest(DbUtils.class)
@RunWith(PowerMockRunner.class)
public class GetDGSGoodsTest extends AbstractUnitTest {

  private GetDGSGoods t;

  private DGSGoodsStoreService mockDGSGoodsStoreService;

  @Before
  public void setUp() {
    mockDGSGoodsStoreService = mock(DGSGoodsStoreService.class);

    t = new GetDGSGoods(mockDGSGoodsStoreService);
  }

  @Test
  public void processRequest_getSellerGoods() throws BurstException {
    final long sellerId = 1L;
    final int firstIndex = 2;
    final int lastIndex = 3;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SELLER_PARAMETER, "" + sellerId),
        new MockParam(FIRST_INDEX_PARAMETER, "" + firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, "" + lastIndex),
        new MockParam(IN_STOCK_ONLY_PARAMETER, "true")
    );

    final Goods mockGood = mockGood();
    final BurstIterator<Goods> mockGoodIterator = mockBurstIterator(mockGood);

    when(mockDGSGoodsStoreService.getSellerGoods(eq(sellerId), eq(true), eq(firstIndex), eq(lastIndex)))
        .thenReturn(mockGoodIterator);

    final JSONObject fullResult = (JSONObject) t.processRequest(req);
    assertNotNull(fullResult);

    final JSONArray goodsList = (JSONArray) fullResult.get(GOODS_RESPONSE);
    assertNotNull(goodsList);
    assertEquals(1, goodsList.size());

    final JSONObject result = (JSONObject) goodsList.get(0);
    assertNotNull(result);

    assertEquals("" + mockGood.getId(), result.get(GOODS_RESPONSE));
    assertEquals(mockGood.getName(), result.get(NAME_RESPONSE));
    assertEquals(mockGood.getDescription(), result.get(DESCRIPTION_RESPONSE));
    assertEquals(mockGood.getQuantity(), result.get(QUANTITY_RESPONSE));
    assertEquals("" + mockGood.getPriceNQT(), result.get(PRICE_NQT_RESPONSE));
    assertEquals("" + mockGood.getSellerId(), result.get(SELLER_PARAMETER));
    assertEquals(mockGood.getTags(), result.get(TAGS_RESPONSE));
    assertEquals(mockGood.isDelisted(), result.get(DELISTED_RESPONSE));
    assertEquals(mockGood.getTimestamp(), result.get(TIMESTAMP_RESPONSE));
  }

  @Test
  public void processRequest_getAllGoods() throws BurstException {
    final long sellerId = 0L;
    final int firstIndex = 2;
    final int lastIndex = 3;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SELLER_PARAMETER, "" + sellerId),
        new MockParam(FIRST_INDEX_PARAMETER, "" + firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, "" + lastIndex),
        new MockParam(IN_STOCK_ONLY_PARAMETER, "false")
    );

    final Goods mockGood = mockGood();
    final BurstIterator<Goods> mockGoodIterator = mockBurstIterator(mockGood);

    when(mockDGSGoodsStoreService.getAllGoods(eq(firstIndex), eq(lastIndex)))
        .thenReturn(mockGoodIterator);

    final JSONObject fullResult = (JSONObject) t.processRequest(req);
    assertNotNull(fullResult);

    final JSONArray goodsList = (JSONArray) fullResult.get(GOODS_RESPONSE);
    assertNotNull(goodsList);
    assertEquals(1, goodsList.size());

    final JSONObject result = (JSONObject) goodsList.get(0);
    assertNotNull(result);

    assertEquals("" + mockGood.getId(), result.get(GOODS_RESPONSE));
    assertEquals(mockGood.getName(), result.get(NAME_RESPONSE));
    assertEquals(mockGood.getDescription(), result.get(DESCRIPTION_RESPONSE));
    assertEquals(mockGood.getQuantity(), result.get(QUANTITY_RESPONSE));
    assertEquals("" + mockGood.getPriceNQT(), result.get(PRICE_NQT_RESPONSE));
    assertEquals("" + mockGood.getSellerId(), result.get(SELLER_PARAMETER));
    assertEquals(mockGood.getTags(), result.get(TAGS_RESPONSE));
    assertEquals(mockGood.isDelisted(), result.get(DELISTED_RESPONSE));
    assertEquals(mockGood.getTimestamp(), result.get(TIMESTAMP_RESPONSE));
  }

  @Test
  public void processRequest_getGoodsInStock() throws BurstException {
    final long sellerId = 0L;
    final int firstIndex = 2;
    final int lastIndex = 3;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SELLER_PARAMETER, "" + sellerId),
        new MockParam(FIRST_INDEX_PARAMETER, "" + firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, "" + lastIndex),
        new MockParam(IN_STOCK_ONLY_PARAMETER, "true")
    );

    final Goods mockGood = mockGood();
    final BurstIterator<Goods> mockGoodIterator = mockBurstIterator(mockGood);

    when(mockDGSGoodsStoreService.getGoodsInStock(eq(firstIndex), eq(lastIndex)))
        .thenReturn(mockGoodIterator);

    final JSONObject fullResult = (JSONObject) t.processRequest(req);
    assertNotNull(fullResult);

    final JSONArray goodsList = (JSONArray) fullResult.get(GOODS_RESPONSE);
    assertNotNull(goodsList);
    assertEquals(1, goodsList.size());

    final JSONObject result = (JSONObject) goodsList.get(0);
    assertNotNull(result);

    assertEquals("" + mockGood.getId(), result.get(GOODS_RESPONSE));
    assertEquals(mockGood.getName(), result.get(NAME_RESPONSE));
    assertEquals(mockGood.getDescription(), result.get(DESCRIPTION_RESPONSE));
    assertEquals(mockGood.getQuantity(), result.get(QUANTITY_RESPONSE));
    assertEquals("" + mockGood.getPriceNQT(), result.get(PRICE_NQT_RESPONSE));
    assertEquals("" + mockGood.getSellerId(), result.get(SELLER_PARAMETER));
    assertEquals(mockGood.getTags(), result.get(TAGS_RESPONSE));
    assertEquals(mockGood.isDelisted(), result.get(DELISTED_RESPONSE));
    assertEquals(mockGood.getTimestamp(), result.get(TIMESTAMP_RESPONSE));
  }

  private DigitalGoodsStore.Goods mockGood() {
    final DigitalGoodsStore.Goods mockGood = mock(DigitalGoodsStore.Goods.class);

    when(mockGood.getId()).thenReturn(1L);
    when(mockGood.getName()).thenReturn("name");
    when(mockGood.getDescription()).thenReturn("description");
    when(mockGood.getQuantity()).thenReturn(2);
    when(mockGood.getPriceNQT()).thenReturn(3L);
    when(mockGood.getSellerId()).thenReturn(4L);
    when(mockGood.getTags()).thenReturn("tags");
    when(mockGood.isDelisted()).thenReturn(true);
    when(mockGood.getTimestamp()).thenReturn(5);

    return mockGood;
  }
}
