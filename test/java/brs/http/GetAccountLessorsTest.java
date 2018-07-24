package brs.http;

import static brs.http.common.ResultFields.ACCOUNT_RESPONSE;
import static brs.http.common.ResultFields.LESSORS_RESPONSE;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountLessorsTest extends AbstractUnitTest {

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;

  private GetAccountLessors t;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);

    t = new GetAccountLessors(parameterServiceMock, blockchainMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(123L);

    final HttpServletRequest req = QuickMocker.httpServletRequest();

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockAccount);
    when(parameterServiceMock.getHeight(eq(req))).thenReturn(0);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);
    assertEquals("" + mockAccount.getId(), result.get(ACCOUNT_RESPONSE));
    assertTrue(((JSONArray) result.get(LESSORS_RESPONSE)).isEmpty());
  }

}
