package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_ASSET_INFO_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.TRANSFERS_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Asset;
import brs.AssetTransfer;
import brs.BurstException;
import brs.assetexchange.AssetExchange;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAssetTransfersTest extends AbstractUnitTest {

  private GetAssetTransfers t;

  private ParameterService mockParameterService;
  private AccountService mockAccountService;
  private AssetExchange mockAssetExchange;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAccountService = mock(AccountService.class);
    mockAssetExchange= mock(AssetExchange.class);

    t = new GetAssetTransfers(mockParameterService, mockAccountService, mockAssetExchange);
  }

  @Test
  public void processRequest_byAsset() throws BurstException {
    final long assetId = 123L;
    final int firstIndex = 0;
    final int lastIndex = 1;
    final boolean includeAssetInfo = true;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ASSET_PARAMETER, assetId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex),
        new MockParam(INCLUDE_ASSET_INFO_PARAMETER, includeAssetInfo)
    );

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(assetId);

    final AssetTransfer mockAssetTransfer = mock(AssetTransfer.class);
    BurstIterator<AssetTransfer> mockAssetTransferIterator = mockBurstIterator(mockAssetTransfer);

    when(mockParameterService.getAsset(eq(req))).thenReturn(mockAsset);

    when(mockAssetExchange.getAssetTransfers(eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockAssetTransferIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);
  }

  @Test
  public void processRequest_byAccount() throws BurstException {
    final long accountId = 234L;
    final int firstIndex = 0;
    final int lastIndex = 1;
    final boolean includeAssetInfo = true;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex),
        new MockParam(INCLUDE_ASSET_INFO_PARAMETER, includeAssetInfo)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);

    final AssetTransfer mockAssetTransfer = mock(AssetTransfer.class);
    BurstIterator<AssetTransfer> mockAssetTransferIterator = mockBurstIterator(mockAssetTransfer);

    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    when(mockAccountService.getAssetTransfers(eq(accountId), eq(firstIndex), eq(lastIndex))).thenReturn(mockAssetTransferIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);
  }

  @Test
  public void processRequest_byAccountAndAsset() throws BurstException {
    final long assetId = 123L;
    final long accountId = 234L;
    final int firstIndex = 0;
    final int lastIndex = 1;
    final boolean includeAssetInfo = true;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(ASSET_PARAMETER, assetId),
      new MockParam(ACCOUNT_PARAMETER, accountId),
      new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
      new MockParam(LAST_INDEX_PARAMETER, lastIndex),
      new MockParam(INCLUDE_ASSET_INFO_PARAMETER, includeAssetInfo)
    );

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(assetId);
    when(mockAsset.getName()).thenReturn("assetName");

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);

    final AssetTransfer mockAssetTransfer = mock(AssetTransfer.class);
    when(mockAssetTransfer.getAssetId()).thenReturn(assetId);
    BurstIterator<AssetTransfer> mockAssetTransferIterator = mockBurstIterator(mockAssetTransfer);

    when(mockParameterService.getAsset(eq(req))).thenReturn(mockAsset);
    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    when(mockAssetExchange.getAsset(eq(mockAssetTransfer.getAssetId()))).thenReturn(mockAsset);

    when(mockAssetExchange.getAccountAssetTransfers(eq(accountId), eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockAssetTransferIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray resultList = (JSONArray) result.get(TRANSFERS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());

    final JSONObject transferInfoResult = (JSONObject) resultList.get(0);
    assertEquals("" + assetId, transferInfoResult.get(ASSET_RESPONSE));
    assertEquals(mockAsset.getName(), transferInfoResult.get(NAME_RESPONSE));
  }

}
