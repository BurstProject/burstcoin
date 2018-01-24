package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

import brs.Account;
import brs.Asset;
import brs.db.BurstIterator;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.ParameterService;
import brs.services.TradeService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAssetsByIssuer extends AbstractAssetsRetrieval {

  private final ParameterService parameterService;
  private final AssetService assetService;

  GetAssetsByIssuer(ParameterService parameterService, AssetService assetService, TradeService tradeService, AssetTransferService assetTransferService, AssetAccountService assetAccountService) {
    super(new APITag[] {APITag.AE, APITag.ACCOUNTS}, tradeService, assetTransferService, assetAccountService, ACCOUNT_PARAMETER, ACCOUNT_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
    this.assetService = assetService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
    List<Account> accounts = parameterService.getAccounts(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONObject response = new JSONObject();
    JSONArray accountsJSONArray = new JSONArray();
    response.put(ASSETS_RESPONSE, accountsJSONArray);
    for (Account account : accounts) {
      try (BurstIterator<Asset> assets = assetService.getAssetsIssuedBy(account.getId(), firstIndex, lastIndex)) {
        accountsJSONArray.add(assetsToJson(assets));
      }
    }
    return response;
  }

}
