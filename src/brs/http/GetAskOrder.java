package brs.http;

import brs.BurstException;
import brs.Order;
import brs.services.OrderService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;

public final class GetAskOrder extends APIServlet.APIRequestHandler {

  private final OrderService orderService;

  GetAskOrder(OrderService orderService) {
    super(new APITag[] {APITag.AE}, ORDER_PARAMETER);
    this.orderService = orderService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Order.Ask askOrder = orderService.getAskOrder(orderId);
    if (askOrder == null) {
      return UNKNOWN_ORDER;
    }
    return JSONData.askOrder(askOrder);
  }

}
