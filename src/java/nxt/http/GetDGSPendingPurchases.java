package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

import static nxt.http.JSONResponses.MISSING_SELLER;

public final class GetDGSPendingPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSPendingPurchases instance = new GetDGSPendingPurchases();

    private GetDGSPendingPurchases() {
        super(new APITag[] {APITag.DGS}, "seller", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long sellerId = ParameterParser.getSellerId(req);
        if (sellerId == null) {
            return MISSING_SELLER;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        Collection<DigitalGoodsStore.Purchase> purchases = DigitalGoodsStore.getPendingSellerPurchases(sellerId);
        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        int i = 0;
        for (DigitalGoodsStore.Purchase purchase : purchases) {
            if (i > lastIndex) {
                break;
            }
            if (i >= firstIndex) {
                purchasesJSON.add(JSONData.purchase(purchase));
            }
            i++;
        }
        response.put("purchases", purchasesJSON);
        return response;
    }

}
