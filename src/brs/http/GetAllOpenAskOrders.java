package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.OPEN_ORDERS_RESPONSE;

import brs.Order;
import brs.assetexchange.AssetExchange;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllOpenAskOrders extends APIServlet.APIRequestHandler {

  private final AssetExchange assetExchange;

  GetAllOpenAskOrders(AssetExchange assetExchange) {
    super(new APITag[] {APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.assetExchange = assetExchange;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    JSONObject response = new JSONObject();
    JSONArray ordersData = new JSONArray();

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    try (BurstIterator<Order.Ask> askOrders = assetExchange.getAllAskOrders(firstIndex, lastIndex)) {
      while (askOrders.hasNext()) {
        ordersData.add(JSONData.askOrder(askOrders.next()));
      }
    }

    response.put(OPEN_ORDERS_RESPONSE, ordersData);
    return response;
  }

}
