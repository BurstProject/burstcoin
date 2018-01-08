package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;

import brs.Account;
import brs.Asset;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAssetAccounts extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAssetAccounts(ParameterService parameterService) {
    super(new APITag[]{APITag.AE}, ASSET_PARAMETER, HEIGHT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Asset asset = parameterService.getAsset(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    int height = parameterService.getHeight(req);

    JSONArray accountAssets = new JSONArray();
    try (BurstIterator<Account.AccountAsset> iterator = asset.getAccounts(height, firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Account.AccountAsset accountAsset = iterator.next();
        accountAssets.add(JSONData.accountAsset(accountAsset));
      }
    }

    JSONObject response = new JSONObject();
    response.put("accountAssets", accountAssets);
    return response;

  }

}
