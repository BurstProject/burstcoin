package brs.http;

import brs.DigitalGoodsStore;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.http.common.Parameters;
import brs.services.DGSGoodsStoreService;
import brs.util.FilteringIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.*;
import static brs.http.common.ResultFields.PURCHASES_RESPONSE;

public final class GetDGSPurchases extends APIServlet.APIRequestHandler {

  private final DGSGoodsStoreService dgsGoodsStoreService;

  public GetDGSPurchases(DGSGoodsStoreService dgsGoodsStoreService) {
    super(new APITag[] {APITag.DGS}, SELLER_PARAMETER, BUYER_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, COMPLETED_PARAMETER);
    this.dgsGoodsStoreService = dgsGoodsStoreService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long sellerId = ParameterParser.getSellerId(req);
    long buyerId = ParameterParser.getBuyerId(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    final boolean completed = Parameters.isTrue(req.getParameter(COMPLETED_PARAMETER));


    JSONObject response = new JSONObject();
    JSONArray purchasesJSON = new JSONArray();
    response.put(PURCHASES_RESPONSE, purchasesJSON);

    if (sellerId == 0 && buyerId == 0) {
      try (FilteringIterator<DigitalGoodsStore.Purchase> purchaseIterator
           = new FilteringIterator<>(dgsGoodsStoreService.getAllPurchases(0, -1),
              purchase -> ! (completed && purchase.isPending()), firstIndex, lastIndex)) {
        while (purchaseIterator.hasNext()) {
          purchasesJSON.add(JSONData.purchase(purchaseIterator.next()));
        }
      }
      return response;
    }

    BurstIterator<DigitalGoodsStore.Purchase> purchases;
    if (sellerId != 0 && buyerId == 0) {
      purchases = dgsGoodsStoreService.getSellerPurchases(sellerId, 0, -1);
    } else if (sellerId == 0) {
      purchases = dgsGoodsStoreService.getBuyerPurchases(buyerId, 0, -1);
    } else {
      purchases = dgsGoodsStoreService.getSellerBuyerPurchases(sellerId, buyerId, 0, -1);
    }
    try (FilteringIterator<DigitalGoodsStore.Purchase> purchaseIterator
         = new FilteringIterator<>(purchases,
            purchase -> ! (completed && purchase.isPending()), firstIndex, lastIndex)) {
      while (purchaseIterator.hasNext()) {
        purchasesJSON.add(JSONData.purchase(purchaseIterator.next()));
      }
    }
    return response;
  }
}
