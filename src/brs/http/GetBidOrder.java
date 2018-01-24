package brs.http;

import brs.BurstException;
import brs.Order;
import brs.services.OrderService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;

public final class GetBidOrder extends APIServlet.APIRequestHandler {

  private final OrderService orderService;

  GetBidOrder(OrderService orderService) {
    super(new APITag[] {APITag.AE}, ORDER_PARAMETER);
    this.orderService = orderService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Order.Bid bidOrder = orderService.getBidOrder(orderId);

    if (bidOrder == null) {
      return UNKNOWN_ORDER;
    }

    return JSONData.bidOrder(bidOrder);
  }

}
