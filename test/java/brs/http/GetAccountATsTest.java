package brs.http;

import static brs.http.common.ResultFields.ATS_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.AT;
import brs.Account;
import brs.BurstException;
import brs.at.AT_Constants;
import brs.at.AT_Machine_State.Machine_State;
import brs.common.QuickMocker;
import brs.services.ATService;
import brs.services.AccountService;
import brs.services.ParameterService;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountATsTest {

  private GetAccountATs t;

  private ParameterService mockParameterService;
  private ATService mockATService;
  private AccountService mockAccountService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockATService = mock(ATService.class);
    mockAccountService = mock(AccountService.class);

    t = new GetAccountATs(mockParameterService, mockATService, mockAccountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final long mockAccountId = 123L;
    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(mockAccountId);

    final long mockATId = 1L;
    byte[] mockATIDBytes = new byte[ AT_Constants.AT_ID_SIZE ];
    byte[] creatorBytes = new byte[]{(byte) 'c', (byte) 'r', (byte) 'e', (byte) 'a', (byte) 't', (byte) 'o', (byte) 'r'};
    final Machine_State mockMachineState = mock(Machine_State.class);
    final AT mockAT = mock(AT.class);
    when(mockAT.getCreator()).thenReturn(creatorBytes);
    when(mockAT.getId()).thenReturn(mockATIDBytes);
    when(mockAT.getMachineState()).thenReturn(mockMachineState);

    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    when(mockAccountService.getAccount(anyLong())).thenReturn(mockAccount);

    when(mockATService.getATsIssuedBy(eq(mockAccountId))).thenReturn(Arrays.asList(mockATId));
    when(mockATService.getAT(eq(mockATId))).thenReturn(mockAT);

    JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray atsResultList = (JSONArray) result.get(ATS_RESPONSE);
    assertNotNull(atsResultList);
    assertEquals(1, atsResultList.size());

    final JSONObject atsResult = (JSONObject) atsResultList.get(0);
    assertNotNull(atsResult);
  }

}
