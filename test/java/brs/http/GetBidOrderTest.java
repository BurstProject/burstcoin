package brs.http;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;
import static brs.http.common.ResultFields.ORDER_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.BurstException;
import brs.Order.Bid;
import brs.assetexchange.AssetExchange;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetBidOrderTest {

  private GetBidOrder t;

  private AssetExchange mockAssetExchange;

  @Before
  public void setUp() {
    mockAssetExchange = mock(AssetExchange.class);

    t = new GetBidOrder(mockAssetExchange);
  }

  @Test
  public void processRequest() throws BurstException {
    final long bidOrderId = 123L;
    Bid mockBid = mock(Bid.class);
    when(mockBid.getId()).thenReturn(bidOrderId);

    when(mockAssetExchange.getBidOrder(eq(bidOrderId))).thenReturn(mockBid);

    HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ORDER_PARAMETER, bidOrderId));

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);
    assertEquals("" + bidOrderId, result.get(ORDER_RESPONSE));
  }

  @Test
  public void processRequest_orderNotFoundUnknownOrder() throws BurstException {
    final long bidOrderId = 123L;

    HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ORDER_PARAMETER, bidOrderId));

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }

}
