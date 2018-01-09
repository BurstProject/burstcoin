package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_ASSET_INFO_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;

import brs.Account;
import brs.Asset;
import brs.AssetTransfer;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.db.sql.DbUtils;
import brs.http.common.Parameters;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAssetTransfers extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AccountService accountService;

  GetAssetTransfers(ParameterService parameterService, AccountService accountService) {
    super(new APITag[]{APITag.AE}, ASSET_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_ASSET_INFO_PARAMETER);
    this.parameterService = parameterService;
    this.accountService = accountService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String assetId = Convert.emptyToNull(req.getParameter(ASSET_PARAMETER));
    String accountId = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    boolean includeAssetInfo = !Parameters.isFalse(req.getParameter(INCLUDE_ASSET_INFO_PARAMETER));

    JSONObject response = new JSONObject();
    JSONArray transfersData = new JSONArray();
    BurstIterator<AssetTransfer> transfers = null;
    try {
      if (accountId == null) {
        Asset asset = parameterService.getAsset(req);
        transfers = asset.getAssetTransfers(firstIndex, lastIndex);
      } else if (assetId == null) {
        Account account = parameterService.getAccount(req);
        transfers = accountService.getAssetTransfers(account.getId(), firstIndex, lastIndex);
      } else {
        Asset asset = parameterService.getAsset(req);
        Account account = parameterService.getAccount(req);
        transfers = AssetTransfer.getAccountAssetTransfers(account.getId(), asset.getId(), firstIndex, lastIndex);
      }
      while (transfers.hasNext()) {
        transfersData.add(JSONData.assetTransfer(transfers.next(), includeAssetInfo));
      }
    } finally {
      DbUtils.close(transfers);
    }
    response.put("transfers", transfersData);

    return response;
  }

  @Override
  boolean startDbTransaction() {
    return true;
  }
}
