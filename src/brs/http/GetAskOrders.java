package brs.http;

import brs.BurstException;
import brs.Order;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAskOrders extends APIServlet.APIRequestHandler {

  static final GetAskOrders instance = new GetAskOrders();

  private GetAskOrders() {
    super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long assetId = ParameterParser.getAsset(req).getId();
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray orders = new JSONArray();
    try (BurstIterator<Order.Ask> askOrders = Order.Ask.getSortedOrders(assetId, firstIndex, lastIndex)) {
      while (askOrders.hasNext()) {
        orders.add(JSONData.askOrder(askOrders.next()));
      }
    }

    JSONObject response = new JSONObject();
    response.put("askOrders", orders);
    return response;

  }

}
