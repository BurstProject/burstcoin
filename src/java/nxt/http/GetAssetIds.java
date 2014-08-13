package nxt.http;

import nxt.Asset;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetIds extends APIServlet.APIRequestHandler {

    static final GetAssetIds instance = new GetAssetIds();

    private GetAssetIds() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray assetIds = new JSONArray();
        for (Asset asset : Asset.getAllAssets()) {
            assetIds.add(Convert.toUnsignedLong(asset.getId()));
        }

        JSONObject response = new JSONObject();
        response.put("assetIds", assetIds);
        return response;
    }

}
