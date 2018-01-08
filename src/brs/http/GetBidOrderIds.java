package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;

import brs.BurstException;
import brs.Order;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetBidOrderIds extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetBidOrderIds(ParameterService parameterService) {
    super(new APITag[]{APITag.AE}, ASSET_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long assetId = parameterService.getAsset(req).getId();
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray orderIds = new JSONArray();
    try (BurstIterator<Order.Bid> bidOrders = Order.Bid.getSortedOrders(assetId, firstIndex, lastIndex)) {
      while (bidOrders.hasNext()) {
        orderIds.add(Convert.toUnsignedLong(bidOrders.next().getId()));
      }
    }
    JSONObject response = new JSONObject();
    response.put("bidOrderIds", orderIds);
    return response;
  }

}
