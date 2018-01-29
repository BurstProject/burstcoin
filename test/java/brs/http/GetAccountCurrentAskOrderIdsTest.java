package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASK_ORDER_IDS_RESPONSE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.BurstException;
import brs.Order.Ask;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.OrderService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountCurrentAskOrderIdsTest extends AbstractUnitTest {

  private GetAccountCurrentAskOrderIds t;

  private ParameterService mockParameterService;
  private OrderService mockOrderService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockOrderService = mock(OrderService.class);

    t = new GetAccountCurrentAskOrderIds(mockParameterService, mockOrderService);
  }

  @Test
  public void processRequest_getAskOrdersByAccount() throws BurstException {
    final long accountId = 2L;
    final int firstIndex = 1;
    final int lastIndex = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);
    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    final Ask mockAsk = mock(Ask.class);
    when(mockAsk.getId()).thenReturn(1L);

    final BurstIterator<Ask> mockAskIterator = mockBurstIterator(mockAsk);

    when(mockOrderService.getAskOrdersByAccount(eq(accountId), eq(firstIndex), eq(lastIndex))).thenReturn(mockAskIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);

    final JSONArray resultList = (JSONArray) result.get(ASK_ORDER_IDS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, (resultList).size());

    assertEquals("" + mockAsk.getId(), resultList.get(0));
  }

  @Test
  public void processRequest_getAskOrdersByAccountAsset() throws BurstException {
    final long assetId = 1L;
    final long accountId = 2L;
    final int firstIndex = 1;
    final int lastIndex = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(ASSET_PARAMETER, assetId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);
    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    final Ask mockAsk = mock(Ask.class);
    when(mockAsk.getId()).thenReturn(1L);

    final BurstIterator<Ask> mockAskIterator = mockBurstIterator(mockAsk);

    when(mockOrderService.getAskOrdersByAccountAsset(eq(accountId), eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockAskIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);

    final JSONArray resultList = (JSONArray) result.get(ASK_ORDER_IDS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, (resultList).size());

    assertEquals("" + mockAsk.getId(), resultList.get(0));
  }

}
