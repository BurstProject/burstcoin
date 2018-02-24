package brs.http;

import static brs.http.JSONResponses.NOT_ENOUGH_ASSETS;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_NQT_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Asset;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class TransferAssetTest extends AbstractTransactionTest {

  private TransferAsset t;

  private ParameterService parameterServiceMock = mock(ParameterService.class);
  private Blockchain blockchainMock = mock(Blockchain.class);
  private AccountService accountServiceMock = mock(AccountService.class);
  private TransactionProcessor transactionProcessorMock = mock(TransactionProcessor.class);
  private TransactionService transactionServiceMock = mock(TransactionService.class);

  @Before
  public void setUp() {
    t = new TransferAsset(parameterServiceMock, transactionProcessorMock, blockchainMock, accountServiceMock, transactionServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final Long assetId = 456L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(RECIPIENT_PARAMETER, "123"),
        new MockParam(ASSET_PARAMETER, "" + assetId),
        new MockParam(QUANTITY_NQT_PARAMETER, "2")
    );

    Asset mockAsset = mock(Asset.class);

    when(parameterServiceMock.getAsset(eq(req))).thenReturn(mockAsset);
    when(mockAsset.getId()).thenReturn(assetId);

    final Account mockSenderAccount = mock(Account.class);
    when(mockSenderAccount.getUnconfirmedAssetBalanceQNT(eq(assetId))).thenReturn(5L);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockSenderAccount);

    prepareTransactionTest(req, parameterServiceMock, transactionProcessorMock, mockSenderAccount);

    t.processRequest(req);
  }

  @Test
  public void processRequest_assetBalanceLowerThanQuantityNQTParameter() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(RECIPIENT_PARAMETER, "123"),
        new MockParam(ASSET_PARAMETER, "456"),
        new MockParam(QUANTITY_NQT_PARAMETER, "5")
    );

    Asset mockAsset = mock(Asset.class);

    when(parameterServiceMock.getAsset(eq(req))).thenReturn(mockAsset);
    when(mockAsset.getId()).thenReturn(456l);

    final Account mockSenderAccount = mock(Account.class);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockSenderAccount);

    when(mockSenderAccount.getUnconfirmedAssetBalanceQNT(anyLong())).thenReturn(2L);

    prepareTransactionTest(req, parameterServiceMock, transactionProcessorMock);

    assertEquals(NOT_ENOUGH_ASSETS, t.processRequest(req));
  }
}
