package brs.http;

import static brs.TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Order.Bid;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.OrderService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class CancelBidOrderTest extends AbstractTransactionTest {

  private CancelBidOrder t;

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;
  private OrderService orderServiceMock;
  private APITransactionManager apiTransactionManagerMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);
    orderServiceMock = mock(OrderService.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new CancelBidOrder(parameterServiceMock, blockchainMock, orderServiceMock, apiTransactionManagerMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final int orderId = 123;
    final long orderAccountId = 1;
    final long senderAccountId = orderAccountId;

    HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    final Bid mockBidOrder = mock(Bid.class);
    when(mockBidOrder.getAccountId()).thenReturn(orderAccountId);
    when(orderServiceMock.getBidOrder(eq(123L))).thenReturn(mockBidOrder);

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(senderAccountId);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockAccount);

    final Attachment.ColoredCoinsBidOrderCancellation attachment = (brs.Attachment.ColoredCoinsBidOrderCancellation) attachmentCreatedTransaction(() -> t.processRequest(req), apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(BID_ORDER_CANCELLATION, attachment.getTransactionType());
    assertEquals(orderId, attachment.getOrderId());
  }

  @Test(expected = ParameterException.class)
  public void processRequest_orderParameterMissing() throws BurstException {
    t.processRequest(QuickMocker.httpServletRequest());
  }

  @Test
  public void processRequest_orderDataMissingUnkownOrder() throws BurstException {
    final int orderId = 123;
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    when(orderServiceMock.getBidOrder(eq(123L))).thenReturn(null);

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }

  @Test
  public void processRequest_accountIdNotSameAsOrder() throws BurstException {
    final int orderId = 123;
    final long orderAccountId = 1;
    final long senderAccountId = 2;

    HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    final Bid mockBidOrder = mock(Bid.class);
    when(mockBidOrder.getAccountId()).thenReturn(orderAccountId);
    when(orderServiceMock.getBidOrder(eq(123L))).thenReturn(mockBidOrder);

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(senderAccountId);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockAccount);

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }

}
