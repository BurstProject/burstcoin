package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASK_ORDER_IDS_RESPONSE;

import brs.BurstException;
import brs.Order;
import brs.db.BurstIterator;
import brs.services.OrderService;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAskOrderIds extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final OrderService orderService;

  GetAskOrderIds(ParameterService parameterService, OrderService orderService) {
    super(new APITag[]{APITag.AE}, ASSET_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
    this.orderService = orderService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long assetId = parameterService.getAsset(req).getId();
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray orderIds = new JSONArray();
    try (BurstIterator<Order.Ask> askOrders = orderService.getSortedOrders(assetId, firstIndex, lastIndex)) {
      while (askOrders.hasNext()) {
        orderIds.add(Convert.toUnsignedLong(askOrders.next().getId()));
      }
    }

    JSONObject response = new JSONObject();
    response.put(ASK_ORDER_IDS_RESPONSE, orderIds);
    return response;

  }

}
