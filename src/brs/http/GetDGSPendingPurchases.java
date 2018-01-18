package brs.http;

import brs.DigitalGoodsStore;
import brs.BurstException;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.MISSING_SELLER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.SELLER_PARAMETER;

public final class GetDGSPendingPurchases extends APIServlet.APIRequestHandler {

  static final GetDGSPendingPurchases instance = new GetDGSPendingPurchases();

  private GetDGSPendingPurchases() {
    super(new APITag[] {APITag.DGS}, SELLER_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long sellerId = ParameterParser.getSellerId(req);
    if (sellerId == 0) {
      return MISSING_SELLER;
    }
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONObject response = new JSONObject();
    JSONArray purchasesJSON = new JSONArray();

    try (BurstIterator<DigitalGoodsStore.Purchase> purchases = DigitalGoodsStore.getPendingSellerPurchases(sellerId, firstIndex, lastIndex)) {
      while (purchases.hasNext()) {
        purchasesJSON.add(JSONData.purchase(purchases.next()));
      }
    }

    response.put("purchases", purchasesJSON);
    return response;
  }

}
