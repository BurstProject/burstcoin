package brs.http;

import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.JSONTestHelper;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.common.TestConstants;
import brs.crypto.Crypto;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class SetRewardRecipientTest extends AbstractTransactionTest {

  private SetRewardRecipient t;

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;
  private AccountService accountServiceMock;
  private APITransactionManager apiTransactionManagerMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);
    accountServiceMock = mock(AccountService.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new SetRewardRecipient(parameterServiceMock, blockchainMock, accountServiceMock, apiTransactionManagerMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(RECIPIENT_PARAMETER, "123"));
    final Account mockSenderAccount = mock(Account.class);
    final Account mockRecipientAccount = mock(Account.class);

    when(mockRecipientAccount.getPublicKey()).thenReturn(Crypto.getPublicKey(TestConstants.TEST_SECRET_PHRASE));

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockSenderAccount);
    when(accountServiceMock.getAccount(eq(123L))).thenReturn(mockRecipientAccount);

    final Attachment.BurstMiningRewardRecipientAssignment attachment = (Attachment.BurstMiningRewardRecipientAssignment) attachmentCreatedTransaction(() -> t.processRequest(req), apiTransactionManagerMock);
    assertNotNull(attachment);
  }

  @Test
  public void processRequest_recipientAccountDoesNotExist_errorCode8() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(RECIPIENT_PARAMETER, "123"));
    final Account mockSenderAccount = mock(Account.class);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockSenderAccount);

    assertEquals(8, JSONTestHelper.errorCode(t.processRequest(req)));
  }

  @Test
  public void processRequest_recipientAccountDoesNotHavePublicKey_errorCode8() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(RECIPIENT_PARAMETER, "123"));
    final Account mockSenderAccount = mock(Account.class);
    final Account mockRecipientAccount = mock(Account.class);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockSenderAccount);
    when(accountServiceMock.getAccount(eq(123L))).thenReturn(mockRecipientAccount);

    assertEquals(8, JSONTestHelper.errorCode(t.processRequest(req)));
  }
}
