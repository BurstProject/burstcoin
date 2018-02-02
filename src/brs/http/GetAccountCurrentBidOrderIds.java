package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.BID_ORDER_IDS_RESPONSE;

import brs.BurstException;
import brs.Order;
import brs.db.BurstIterator;
import brs.services.OrderService;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountCurrentBidOrderIds extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final OrderService orderService;

  GetAccountCurrentBidOrderIds(ParameterService parameterService, OrderService orderService) {
    super(new APITag[] {APITag.ACCOUNTS, APITag.AE}, ACCOUNT_PARAMETER, ASSET_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
    this.orderService = orderService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long accountId = parameterService.getAccount(req).getId();
    long assetId = 0;
    try {
      assetId = Convert.parseUnsignedLong(req.getParameter(ASSET_PARAMETER));
    } catch (RuntimeException e) {
      // ignore
    }
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    BurstIterator<Order.Bid> bidOrders;
    if (assetId == 0) {
      bidOrders = orderService.getBidOrdersByAccount(accountId, firstIndex, lastIndex);
    } else {
      bidOrders = orderService.getBidOrdersByAccountAsset(accountId, assetId, firstIndex, lastIndex);
    }
    JSONArray orderIds = new JSONArray();
    try {
      while (bidOrders.hasNext()) {
        orderIds.add(Convert.toUnsignedLong(bidOrders.next().getId()));
      }
    } finally {
      bidOrders.close();
    }
    JSONObject response = new JSONObject();
    response.put(BID_ORDER_IDS_RESPONSE, orderIds);
    return response;
  }

}
