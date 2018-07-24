package brs.peer;

import static brs.common.TestConstants.TEST_ACCOUNT_ID;
import static brs.common.TestConstants.TEST_ACCOUNT_NUMERIC_ID_PARSED;
import static brs.peer.GetAccountBalance.ACCOUNT_ID_PARAMETER_FIELD;
import static brs.peer.GetAccountBalance.BALANCE_NQT_RESPONSE_FIELD;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.services.AccountService;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountBalanceTest {

  private GetAccountBalance t;

  private AccountService mockAccountService;

  @Before
  public void setUp() {
    mockAccountService = mock(AccountService.class);

    t = new GetAccountBalance(mockAccountService);
  }

  @Test
  public void processRequest() {
    final JSONObject req = new JSONObject();
    req.put(ACCOUNT_ID_PARAMETER_FIELD, TEST_ACCOUNT_ID);
    final Peer peer = mock(Peer.class);

    long mockBalanceNQT = 5;
    Account mockAccount = mock(Account.class);
    when(mockAccount.getBalanceNQT()).thenReturn(mockBalanceNQT);

    when(mockAccountService.getAccount(eq(TEST_ACCOUNT_NUMERIC_ID_PARSED))).thenReturn(mockAccount);

    final JSONObject result = (JSONObject) t.processRequest(req, peer);

    assertEquals("" + mockBalanceNQT, result.get(BALANCE_NQT_RESPONSE_FIELD));
  }

  @Test
  public void processRequest_notExistingAccount() {
    final JSONObject req = new JSONObject();
    req.put(ACCOUNT_ID_PARAMETER_FIELD, TEST_ACCOUNT_ID);
    final Peer peer = mock(Peer.class);

    when(mockAccountService.getAccount(eq(TEST_ACCOUNT_NUMERIC_ID_PARSED))).thenReturn(null);

    final JSONObject result = (JSONObject) t.processRequest(req, peer);

    assertEquals("0", result.get(BALANCE_NQT_RESPONSE_FIELD));
  }

}
