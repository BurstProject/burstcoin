package brs.http;

import brs.BurstException;
import brs.Order;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;

public final class GetBidOrder extends APIServlet.APIRequestHandler {

  static final GetBidOrder instance = new GetBidOrder();

  private GetBidOrder() {
    super(new APITag[] {APITag.AE}, "order");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Order.Bid bidOrder = Order.Bid.getBidOrder(orderId);
    if (bidOrder == null) {
      return UNKNOWN_ORDER;
    }
    return JSONData.bidOrder(bidOrder);
  }

}
