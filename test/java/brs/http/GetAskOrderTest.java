package brs.http;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.BurstException;
import brs.Order.Ask;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.OrderService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.junit.Before;
import org.junit.Test;

public class GetAskOrderTest {

  private GetAskOrder t;

  private OrderService mockOrderService;

  @Before
  public void setUp() {
    mockOrderService = mock(OrderService.class);

    t = new GetAskOrder(mockOrderService);
  }

  @Test
  public void processRequest() throws BurstException {
    final long orderId = 123L;

    final Ask mockOrder = mock(Ask.class);

    when(mockOrderService.getAskOrder(eq(orderId))).thenReturn(mockOrder);

    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(ORDER_PARAMETER, orderId)
    );

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);
  }

  @Test
  public void processRequest_unknownOrder() throws BurstException {
    final long orderId = 123L;

    when(mockOrderService.getAskOrder(eq(orderId))).thenReturn(null);

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }

}
