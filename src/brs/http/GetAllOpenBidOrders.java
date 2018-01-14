package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;

import brs.Order;
import brs.db.BurstIterator;
import brs.services.OrderService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllOpenBidOrders extends APIServlet.APIRequestHandler {

  private final OrderService orderService;

  GetAllOpenBidOrders(OrderService orderService) {
    super(new APITag[] {APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.orderService = orderService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    JSONObject response = new JSONObject();
    JSONArray ordersData = new JSONArray();

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    try (BurstIterator<Order.Bid> bidOrders = orderService.getAllBidOrders(firstIndex, lastIndex)) {
      while (bidOrders.hasNext()) {
        ordersData.add(JSONData.bidOrder(bidOrders.next()));
      }
    }

    response.put("openOrders", ordersData);
    return response;
  }

}
