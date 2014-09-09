package nxt.http;

import nxt.Account;
import nxt.Asset;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAssetsByIssuer extends APIServlet.APIRequestHandler {

    static final GetAssetsByIssuer instance = new GetAssetsByIssuer();

    private GetAssetsByIssuer() {
        super(new APITag[] {APITag.AE, APITag.ACCOUNTS}, "account", "account", "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        List<Account> accounts = ParameterParser.getAccounts(req);
        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        response.put("assets", accountsJSONArray);
        for (Account account : accounts) {
            List<Asset> assets = Asset.getAssetsIssuedBy(account.getId());
            JSONArray assetsJSONArray = new JSONArray();
            for (Asset asset : assets) {
                assetsJSONArray.add(JSONData.asset(asset));
            }
            accountsJSONArray.add(assetsJSONArray);
        }
        return response;
    }

}
