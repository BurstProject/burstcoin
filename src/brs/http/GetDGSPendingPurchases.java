package brs.http;

import brs.DigitalGoodsStore;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.services.DGSGoodsStoreService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.MISSING_SELLER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.SELLER_PARAMETER;
import static brs.http.common.ResultFields.PURCHASES_RESPONSE;

public final class GetDGSPendingPurchases extends APIServlet.APIRequestHandler {

  private final DGSGoodsStoreService dgsGoodStoreService;

  GetDGSPendingPurchases(DGSGoodsStoreService dgsGoodStoreService) {
    super(new APITag[] {APITag.DGS}, SELLER_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.dgsGoodStoreService = dgsGoodStoreService;
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

    try (BurstIterator<DigitalGoodsStore.Purchase> purchases = dgsGoodStoreService.getPendingSellerPurchases(sellerId, firstIndex, lastIndex)) {
      while (purchases.hasNext()) {
        purchasesJSON.add(JSONData.purchase(purchases.next()));
      }
    }

    response.put(PURCHASES_RESPONSE, purchasesJSON);
    return response;
  }

}
