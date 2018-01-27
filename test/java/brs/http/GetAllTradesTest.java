package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_ASSET_INFO_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.PRICE_NQT_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.PRICE_NQT_RESPONSE;
import static brs.http.common.ResultFields.TRADES_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.BurstException;
import brs.Trade;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.AssetService;
import brs.services.TradeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAllTradesTest extends AbstractUnitTest {

  private GetAllTrades t;

  private TradeService mockTradeService;
  private AssetService mockAssetService;

  @Before
  public void setUp() {
    mockTradeService = mock(TradeService.class);
    mockAssetService = mock(AssetService.class);

    t = new GetAllTrades(mockTradeService, mockAssetService);
  }

  @Test
  public void processRequest_withAssetsInformation() throws BurstException {
    final int timestamp = 1;
    final int firstIndex = 0;
    final int lastIndex = 1;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(TIMESTAMP_PARAMETER, timestamp),
      new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
      new MockParam(LAST_INDEX_PARAMETER, lastIndex),
      new MockParam(INCLUDE_ASSET_INFO_PARAMETER, true)
    );

    final long mockAssetId = 123L;
    final String mockAssetName = "mockAssetName";
    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(mockAssetId);
    when(mockAsset.getName()).thenReturn(mockAssetName);

    final long priceNQT = 123L;
    final Trade mockTrade = mock(Trade.class);
    when(mockTrade.getPriceNQT()).thenReturn(priceNQT);
    when(mockTrade.getTimestamp()).thenReturn(2);
    when(mockTrade.getAssetId()).thenReturn(mockAssetId);

    final BurstIterator<Trade> mockTradeIterator = mockBurstIterator(mockTrade);

    when(mockTradeService.getAllTrades(eq(0), eq(-1))).thenReturn(mockTradeIterator);
    when(mockAssetService.getAsset(eq(mockAssetId))).thenReturn(mockAsset);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray tradesResult = (JSONArray) result.get(TRADES_RESPONSE);
    assertNotNull(tradesResult);
    assertEquals(1, tradesResult.size());

    final JSONObject tradeAssetInfoResult = (JSONObject) tradesResult.get(0);
    assertNotNull(tradeAssetInfoResult);

    assertEquals("" + priceNQT, tradeAssetInfoResult.get(PRICE_NQT_RESPONSE));
    assertEquals("" + mockAssetId, tradeAssetInfoResult.get(ASSET_RESPONSE));
    assertEquals(mockAssetName, tradeAssetInfoResult.get(NAME_RESPONSE));
  }

  @Test
  public void processRequest_withoutAssetsInformation() throws BurstException {
    final int timestamp = 1;
    final int firstIndex = 0;
    final int lastIndex = 1;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(TIMESTAMP_PARAMETER, timestamp),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex),
        new MockParam(INCLUDE_ASSET_INFO_PARAMETER, false)
    );

    final long mockAssetId = 123L;
    final long priceNQT = 123L;
    final Trade mockTrade = mock(Trade.class);
    when(mockTrade.getPriceNQT()).thenReturn(priceNQT);
    when(mockTrade.getTimestamp()).thenReturn(2);
    when(mockTrade.getAssetId()).thenReturn(mockAssetId);

    final BurstIterator<Trade> mockTradeIterator = mockBurstIterator(mockTrade);

    when(mockTradeService.getAllTrades(eq(0), eq(-1))).thenReturn(mockTradeIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray tradesResult = (JSONArray) result.get(TRADES_RESPONSE);
    assertNotNull(tradesResult);
    assertEquals(1, tradesResult.size());

    final JSONObject tradeAssetInfoResult = (JSONObject) tradesResult.get(0);
    assertNotNull(tradeAssetInfoResult);

    assertEquals("" + priceNQT, tradeAssetInfoResult.get(PRICE_NQT_RESPONSE));
    assertEquals("" + mockAssetId, tradeAssetInfoResult.get(ASSET_RESPONSE));
    assertEquals(null, tradeAssetInfoResult.get(NAME_RESPONSE));

    verify(mockAssetService, never()).getAsset(eq(mockAssetId));
  }

}
