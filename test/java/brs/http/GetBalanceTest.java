package brs.http;

import static brs.http.common.ResultFields.BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.EFFECTIVE_BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.FORGED_BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.GUARANTEED_BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_BALANCE_NQT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.BurstException;
import brs.common.QuickMocker;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetBalanceTest {

  private GetBalance t;

  private ParameterService parameterServiceMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    this.t = new GetBalance(parameterServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    Account mockAccount = mock(Account.class);

    when(mockAccount.getBalanceNQT()).thenReturn(1L);
    when(mockAccount.getUnconfirmedBalanceNQT()).thenReturn(2L);
    when(mockAccount.getForgedBalanceNQT()).thenReturn(3L);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockAccount);

    JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals("1", result.get(BALANCE_NQT_RESPONSE));
    assertEquals("2", result.get(UNCONFIRMED_BALANCE_NQT_RESPONSE));
    assertEquals("1", result.get(EFFECTIVE_BALANCE_NQT_RESPONSE));
    assertEquals("3", result.get(FORGED_BALANCE_NQT_RESPONSE));
    assertEquals("1", result.get(GUARANTEED_BALANCE_NQT_RESPONSE));
  }

  @Test
  public void processRequest_noAccountFound() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(null);

    JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals("0", result.get(BALANCE_NQT_RESPONSE));
    assertEquals("0", result.get(UNCONFIRMED_BALANCE_NQT_RESPONSE));
    assertEquals("0", result.get(EFFECTIVE_BALANCE_NQT_RESPONSE));
    assertEquals("0", result.get(FORGED_BALANCE_NQT_RESPONSE));
    assertEquals("0", result.get(GUARANTEED_BALANCE_NQT_RESPONSE));
  }
}
