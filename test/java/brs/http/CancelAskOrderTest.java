package brs.http;

import static org.powermock.api.mockito.PowerMockito.mock;

import brs.Blockchain;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import org.junit.Before;
import org.junit.Test;

public class CancelAskOrderTest extends AbstractCreateTransactionTest {

  private CancelAskOrder t;

  private ParameterService parameterServiceMock;
  private TransactionProcessor transactionProcessorMock;
  private Blockchain blockchainMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    transactionProcessorMock = mock(TransactionProcessor.class);
    blockchainMock = mock(Blockchain.class);

    t = new CancelAskOrder(parameterServiceMock, transactionProcessorMock, blockchainMock);
  }

  @Test
  public void processRequest() {
    //TODO Add tests
  }

  /*
  @Test
  public void processRequest_unknownOrderDueNoOrderData() throws BurstException {
    System.out.println(getHex(Crypto.getPublicKey(TestConstants.TEST_SECRET_PHRASE)));

    final HttpServletRequest req = QuickMocker.httpServletRequestDefaultKeys(
        new MockParam(ORDER_PARAMETER, "3"));

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }
  */

  private static final String HEXES = "0123456789ABCDEF";

  static String getHex(byte[] raw) {
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final byte b : raw) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
  }
/*
  @Test
  public void processRequest_unknownOrderDueWrongAccountId() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequestDefaultKeys(
        new MockParam(ORDER_PARAMETER, "3"));

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }
*/

}
