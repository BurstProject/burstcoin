package brs.http;

import static brs.http.common.ResultFields.DONE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import brs.TransactionProcessor;
import brs.common.QuickMocker;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ClearUnconfirmedTransactionsTest {

  private ClearUnconfirmedTransactions t;

  private TransactionProcessor transactionProcessorMock;

  @Before
  public void init() {
    transactionProcessorMock = mock(TransactionProcessor.class);

    this.t = new ClearUnconfirmedTransactions(transactionProcessorMock);
  }

  @Test
  public void processRequest() {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final JSONObject result = ((JSONObject) t.processRequest(req));

    assertEquals(true, result.get(DONE_RESPONSE));
  }

  @Test
  public void processRequest_runtimeExceptionOccurs() {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    doThrow(new RuntimeException("errorMessage")).when(transactionProcessorMock).clearUnconfirmedTransactions();

    final JSONObject result = ((JSONObject) t.processRequest(req));

    assertEquals("java.lang.RuntimeException: errorMessage", result.get(ERROR_RESPONSE));
  }

  @Test
  public void requirePost() {
    assertTrue(t.requirePost());
  }
}