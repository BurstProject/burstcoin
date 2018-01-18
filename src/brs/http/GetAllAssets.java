package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

import brs.Asset;
import brs.db.BurstIterator;
import brs.services.AssetService;
import brs.services.impl.AssetServiceImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllAssets extends APIServlet.APIRequestHandler {

  private final AssetService assetService;

  public GetAllAssets(AssetService assetService) {
    super(new APITag[] {APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.assetService = assetService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONObject response = new JSONObject();
    JSONArray assetsJSONArray = new JSONArray();
    response.put(ASSETS_RESPONSE, assetsJSONArray);
    try (BurstIterator<Asset> assets = assetService.getAllAssets(firstIndex, lastIndex)) {
      while (assets.hasNext()) {
        assetsJSONArray.add(JSONData.asset(assets.next()));
      }
    }
    return response;
  }

}
