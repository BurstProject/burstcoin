package brs.http;

import static brs.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static brs.http.JSONResponses.INCORRECT_ALIAS_NAME;
import static brs.http.JSONResponses.INCORRECT_URI_LENGTH;
import static brs.http.JSONResponses.MISSING_ALIAS_NAME;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_URI_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class SetAliasTest extends AbstractTransactionTest {

  private SetAlias t;

  private ParameterService parameterServiceMock;
  private TransactionProcessor transactionProcessorMock;
  private Blockchain blockchainMock;
  private AccountService accountServiceMock;
  private AliasService aliasServiceMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    transactionProcessorMock = mock(TransactionProcessor.class);
    blockchainMock = mock(Blockchain.class);
    accountServiceMock = mock(AccountService.class);
    aliasServiceMock = mock(AliasService.class);

    t = new SetAlias(parameterServiceMock, transactionProcessorMock, blockchainMock, accountServiceMock, aliasServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final Account mockSenderAccount = mock(Account.class);
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ALIAS_NAME_PARAMETER, "aliasName"),
        new MockParam(ALIAS_URI_PARAMETER, "aliasUrl")
    );

    prepareTransactionTest(req, parameterServiceMock, transactionProcessorMock, mockSenderAccount);

    t.processRequest(req);
  }

  @Test
  public void processRequest_missingAliasName() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ALIAS_NAME_PARAMETER, null),
        new MockParam(ALIAS_URI_PARAMETER, "aliasUrl")
    );

    assertEquals(MISSING_ALIAS_NAME, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectAliasLength_nameOnlySpaces() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ALIAS_NAME_PARAMETER, "  "),
        new MockParam(ALIAS_URI_PARAMETER, null)
    );

    assertEquals(INCORRECT_ALIAS_LENGTH, t.processRequest(req));
  }


  @Test
  public void processRequest_incorrectAliasLength_incorrectAliasName() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ALIAS_NAME_PARAMETER, "[]"),
        new MockParam(ALIAS_URI_PARAMETER, null)
    );

    assertEquals(INCORRECT_ALIAS_NAME, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectUriLengthWhenOver1000Characters() throws BurstException {
    final StringBuilder uriOver1000Characters = new StringBuilder();

    for (int i = 0; i < 1001; i++) {
      uriOver1000Characters.append("a");
    }

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ALIAS_NAME_PARAMETER, "name"),
        new MockParam(ALIAS_URI_PARAMETER, uriOver1000Characters.toString())
    );

    assertEquals(INCORRECT_URI_LENGTH, t.processRequest(req));
  }

}
