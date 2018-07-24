package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.BID_ORDERS_RESPONSE;
import static brs.http.common.ResultFields.ORDER_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.BurstException;
import brs.Order.Bid;
import brs.assetexchange.AssetExchange;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetBidOrdersTest extends AbstractUnitTest {

  private GetBidOrders t;

  private ParameterService mockParameterService;
  private AssetExchange mockAssetExchange;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAssetExchange = mock(AssetExchange.class);

    t = new GetBidOrders(mockParameterService, mockAssetExchange);
  }

  @Test
  public void processRequest() throws BurstException {
    final long assetId = 123L;
    final int firstIndex = 0;
    final int lastIndex = 1;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(assetId);

    long mockOrderId = 345L;
    final Bid mockBid = mock(Bid.class);
    when(mockBid.getId()).thenReturn(mockOrderId);

    final BurstIterator<Bid> mockBidIterator = mockBurstIterator(mockBid);

    when(mockParameterService.getAsset(req)).thenReturn(mockAsset);
    when(mockAssetExchange.getSortedBidOrders(eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockBidIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray resultBidOrdersList = (JSONArray) result.get(BID_ORDERS_RESPONSE);
    assertNotNull(resultBidOrdersList);
    assertEquals(1, resultBidOrdersList.size());

    final JSONObject resultBidOrder = (JSONObject) resultBidOrdersList.get(0);
    assertNotNull(resultBidOrder);

    assertEquals("" + mockOrderId, resultBidOrder.get(ORDER_RESPONSE));
  }

}
