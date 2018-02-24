package brs.http;

import static brs.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static brs.http.JSONResponses.INCORRECT_ALIAS_NAME;
import static brs.http.JSONResponses.INCORRECT_URI_LENGTH;
import static brs.http.JSONResponses.MISSING_ALIAS_NAME;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_URI_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class SetAliasTest extends AbstractTransactionTest {

  private SetAlias t;

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;
  private AliasService aliasServiceMock;
  private APITransactionManager apiTransactionManagerMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);
    aliasServiceMock = mock(AliasService.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new SetAlias(parameterServiceMock, blockchainMock, aliasServiceMock, apiTransactionManagerMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ALIAS_NAME_PARAMETER, "aliasName"),
        new MockParam(ALIAS_URI_PARAMETER, "aliasUrl")
    );

    final Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) attachmentCreatedTransaction(() -> t.processRequest(req), apiTransactionManagerMock);
    assertNotNull(attachment);
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
