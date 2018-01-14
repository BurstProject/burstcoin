package brs.http;

import static org.powermock.api.mockito.PowerMockito.mock;

import brs.Blockchain;
import brs.TransactionProcessor;
import brs.services.AccountService;
import brs.services.OrderService;
import brs.services.ParameterService;
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
  public void processRequest() {
    //TODO Add tests
  }

}
