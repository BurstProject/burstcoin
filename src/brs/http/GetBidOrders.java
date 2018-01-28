package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.BID_ORDERS_RESPONSE;

import brs.BurstException;
import brs.Order;
import brs.db.BurstIterator;
import brs.services.OrderService;
import brs.services.ParameterService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBidOrders extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final OrderService orderService;

  GetBidOrders(ParameterService parameterService, OrderService orderService) {
    super(new APITag[] {APITag.AE}, ASSET_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
    this.orderService = orderService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long assetId = parameterService.getAsset(req).getId();
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray orders = new JSONArray();
    try (BurstIterator<Order.Bid> bidOrders = orderService.getSortedBidOrders(assetId, firstIndex, lastIndex)) {
      while (bidOrders.hasNext()) {
        orders.add(JSONData.bidOrder(bidOrders.next()));
      }
    }

    JSONObject response = new JSONObject();
    response.put(BID_ORDERS_RESPONSE, orders);
    return response;
  }

}
