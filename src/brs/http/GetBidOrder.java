package brs.http;

import brs.BurstException;
import brs.Order;
import brs.assetexchange.AssetExchange;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;

public final class GetBidOrder extends APIServlet.APIRequestHandler {

  private final AssetExchange assetExchange;

  GetBidOrder(AssetExchange assetExchange) {
    super(new APITag[] {APITag.AE}, ORDER_PARAMETER);
    this.assetExchange = assetExchange;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Order.Bid bidOrder = assetExchange.getBidOrder(orderId);

    if (bidOrder == null) {
      return UNKNOWN_ORDER;
    }

    return JSONData.bidOrder(bidOrder);
  }

}
