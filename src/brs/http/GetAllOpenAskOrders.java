package brs.http;

import brs.Order;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllOpenAskOrders extends APIServlet.APIRequestHandler {

  static final GetAllOpenAskOrders instance = new GetAllOpenAskOrders();

  private GetAllOpenAskOrders() {
    super(new APITag[] {APITag.AE}, "firstIndex", "lastIndex");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    JSONObject response = new JSONObject();
    JSONArray ordersData = new JSONArray();

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    try (BurstIterator<Order.Ask> askOrders = Order.Ask.getAll(firstIndex, lastIndex)) {
      while (askOrders.hasNext()) {
        ordersData.add(JSONData.askOrder(askOrders.next()));
      }
    }

    response.put("openOrders", ordersData);
    return response;
  }

}
