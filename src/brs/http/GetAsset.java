package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;

import brs.Asset;
import brs.BurstException;
import brs.services.AssetAccountService;
import brs.services.AssetTransferService;
import brs.services.ParameterService;
import brs.services.TradeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class GetAsset extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AssetAccountService assetAccountService;
  private final AssetTransferService assetTransferService;
  private final TradeService tradeService;

  GetAsset(ParameterService parameterService, AssetAccountService assetAccountService, AssetTransferService assetTransferService, TradeService tradeService) {
    super(new APITag[]{APITag.AE}, ASSET_PARAMETER);
    this.parameterService = parameterService;
    this.assetAccountService = assetAccountService;
    this.assetTransferService = assetTransferService;
    this.tradeService = tradeService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final Asset asset = parameterService.getAsset(req);

    int tradeCount = tradeService.getTradeCount(asset.getId());
    int transferCount = assetTransferService.getTransferCount(asset.getId());
    int accountsCount = assetAccountService.getAssetAccountsCount(asset.getId());

    return JSONData.asset(asset, tradeCount, transferCount, accountsCount);
  }

}
