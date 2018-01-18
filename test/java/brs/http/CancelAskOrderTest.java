package brs.http;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.Order.Ask;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.OrderService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;
import org.junit.Before;
import org.junit.Test;

public class CancelAskOrderTest extends AbstractTransactionTest {

  private CancelAskOrder t;

  private ParameterService parameterServiceMock;
  private TransactionProcessor transactionProcessorMock;
  private Blockchain blockchainMock;
  private AccountService accountServiceMock;
  private OrderService orderServiceMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    transactionProcessorMock = mock(TransactionProcessor.class);
    blockchainMock = mock(Blockchain.class);
    accountServiceMock = mock(AccountService.class);
    orderServiceMock = mock(OrderService.class);

    t = new CancelAskOrder(parameterServiceMock, transactionProcessorMock, blockchainMock, accountServiceMock, orderServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final String orderId = "123";
    final long orderAccountId = 1;
    final long senderAccountId = orderAccountId;

    HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    final Ask mockAskOrder = mock(Ask.class);
    when(mockAskOrder.getAccountId()).thenReturn(orderAccountId);
    when(orderServiceMock.getAskOrder(eq(123L))).thenReturn(mockAskOrder);

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(senderAccountId);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockAccount);

    t.processRequest(req);
  }

  @Test(expected = ParameterException.class)
  public void processRequest_orderParameterMissing() throws BurstException {
    t.processRequest(QuickMocker.httpServletRequest());
  }

  @Test
  public void processRequest_orderDataMissingUnkownOrder() throws BurstException {
    final String orderId = "123";
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    when(orderServiceMock.getAskOrder(eq(123L))).thenReturn(null);

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }

  @Test
  public void processRequest_accountIdNotSameAsOrder() throws BurstException {
    final String orderId = "123";
    final long orderAccountId = 1;
    final long senderAccountId = 2;

    HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    final Ask mockAskOrder = mock(Ask.class);
    when(mockAskOrder.getAccountId()).thenReturn(orderAccountId);
    when(orderServiceMock.getAskOrder(eq(123L))).thenReturn(mockAskOrder);

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(senderAccountId);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockAccount);

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }

}
