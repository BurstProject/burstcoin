package nxt.http;

import nxt.NxtException;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountCurrentAskOrderIds extends APIServlet.APIRequestHandler {

    static final GetAccountCurrentAskOrderIds instance = new GetAccountCurrentAskOrderIds();

    private GetAccountCurrentAskOrderIds() {
        super("account", "asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long accountId = ParameterParser.getAccount(req).getId();
        Long assetId = null;
        try {
            assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
        } catch (RuntimeException e) {
            // ignore
        }

        JSONArray orderIds = new JSONArray();
        for (Order.Ask askOrder : Order.Ask.getAllAskOrders()) {
            if ((assetId == null || askOrder.getAssetId().equals(assetId)) && askOrder.getAccount().getId().equals(accountId)) {
                orderIds.add(Convert.toUnsignedLong(askOrder.getId()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("askOrderIds", orderIds);
        return response;
    }

}
