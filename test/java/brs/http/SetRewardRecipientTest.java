package brs.http;

import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Attachment.BurstMiningRewardRecipientAssignment;
import brs.Block;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.JSONTestHelper;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.common.TestConstants;
import brs.crypto.Crypto;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.junit.Before;
import org.junit.Test;
import org.omg.IOP.TransactionService;

public class SetRewardRecipientTest extends AbstractTransactionTest {

  private SetRewardRecipient t;

  private ParameterService parameterServiceMock = mock(ParameterService.class);
  private Blockchain blockchainMock = mock(Blockchain.class);
  private AccountService accountServiceMock = mock(AccountService.class);
  private TransactionService transactionServiceMock = mock(TransactionService.class);
  private TransactionProcessor transactionProcessorMock = mock(TransactionProcessor.class);
  private AliasService aliasServiceMock = mock(AliasService.class);

  @Before
  public void setUp() {
    t = new SetRewardRecipient(parameterServiceMock, transactionProcessorMock, blockchainMock, accountServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(RECIPIENT_PARAMETER, "123"));
    final Account mockSenderAccount = mock(Account.class);
    final Account mockRecipientAccount = mock(Account.class);

    when(mockRecipientAccount.getPublicKey()).thenReturn(Crypto.getPublicKey(TestConstants.TEST_SECRET_PHRASE));

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockSenderAccount);
    when(accountServiceMock.getAccount(eq(123L))).thenReturn(mockRecipientAccount);

    prepareTransactionTest(req, parameterServiceMock, transactionProcessorMock, aliasServiceMock);

    t.processRequest(req);
  }

  @Test
  public void processRequest_recipientAccountDoesNotExist_errorCode8() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(RECIPIENT_PARAMETER, "123"));
    final Account mockSenderAccount = mock(Account.class);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockSenderAccount);

    prepareTransactionTest(req, parameterServiceMock, transactionProcessorMock, aliasServiceMock);

    assertEquals(8, JSONTestHelper.errorCode(t.processRequest(req)));
  }

  @Test
  public void processRequest_recipientAccountDoesNotHavePublicKey_errorCode8() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(RECIPIENT_PARAMETER, "123"));
    final Account mockSenderAccount = mock(Account.class);
    final Account mockRecipientAccount = mock(Account.class);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockSenderAccount);
    when(accountServiceMock.getAccount(eq(123L))).thenReturn(mockRecipientAccount);

    prepareTransactionTest(req, parameterServiceMock, transactionProcessorMock, aliasServiceMock);

    assertEquals(8, JSONTestHelper.errorCode(t.processRequest(req)));
  }
}