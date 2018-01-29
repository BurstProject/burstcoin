package brs.http;

import static brs.http.JSONResponses.INCORRECT_ASSET;
import static brs.http.JSONResponses.UNKNOWN_ASSET;
import static brs.http.common.Parameters.ASSETS_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_ACCOUNTS_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRADES_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRANSFERS_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.TradeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAssetsTest {

  private GetAssets t;

  private AssetService mockAssetService;
  private AssetAccountService mockAssetAccountService;
  private AssetTransferService mockAssetTransferService;
  private TradeService mockTradeService;

  @Before
  public void setUp() {
    mockAssetService = mock(AssetService.class);
    mockAssetAccountService = mock(AssetAccountService.class);
    mockAssetTransferService = mock(AssetTransferService.class);
    mockTradeService = mock(TradeService.class);

    t = new GetAssets(mockAssetService, mockAssetAccountService, mockAssetTransferService, mockTradeService);
  }

  @Test
  public void processRequest() {
    final long assetId = 123L;

    final HttpServletRequest req = QuickMocker.httpServletRequest();
    when(req.getParameterValues(eq(ASSETS_PARAMETER))).thenReturn(new String[]{"" + assetId, ""});

    final int mockTradeCount = 1;
    final int mockTransferCount = 2;
    final int mockAccountsCount = 3;

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(assetId);

    when(mockAssetService.getAsset(eq(assetId))).thenReturn(mockAsset);

    when(mockTradeService.getTradeCount(eq(assetId))).thenReturn(mockTradeCount);
    when(mockAssetTransferService.getTransferCount(eq(assetId))).thenReturn(mockTransferCount);
    when(mockAssetAccountService.getAssetAccountsCount(eq(assetId))).thenReturn(mockAccountsCount);

    final JSONObject response = (JSONObject) t.processRequest(req);
    assertNotNull(response);

    final JSONArray responseList = (JSONArray) response.get(ASSETS_RESPONSE);
    assertNotNull(responseList);
    assertEquals(1, responseList.size());

    final JSONObject assetResponse = (JSONObject) responseList.get(0);
    assertNotNull(assetResponse);
    assertEquals(mockTradeCount, assetResponse.get(NUMBER_OF_TRADES_RESPONSE));
    assertEquals(mockTransferCount, assetResponse.get(NUMBER_OF_TRANSFERS_RESPONSE));
    assertEquals(mockAccountsCount, assetResponse.get(NUMBER_OF_ACCOUNTS_RESPONSE));
  }

  @Test
  public void processRequest_unknownAsset() {
    final long assetId = 123L;

    final HttpServletRequest req = QuickMocker.httpServletRequest();
    when(req.getParameterValues(eq(ASSETS_PARAMETER))).thenReturn(new String[]{"" + assetId});

    when(mockAssetService.getAsset(eq(assetId))).thenReturn(null);

    assertEquals(UNKNOWN_ASSET, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectAsset() {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    when(req.getParameterValues(eq(ASSETS_PARAMETER))).thenReturn(new String[]{"unParsable"});

    assertEquals(INCORRECT_ASSET, t.processRequest(req));
  }

}
