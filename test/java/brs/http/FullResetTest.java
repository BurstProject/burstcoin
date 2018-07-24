package brs.http;

import static brs.http.common.ResultFields.DONE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import brs.BlockchainProcessor;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class FullResetTest {

  private FullReset t;

  private BlockchainProcessor blockchainProcessor;

  @Before
  public void init() {
    blockchainProcessor = mock(BlockchainProcessor.class);

    this.t = new FullReset(blockchainProcessor);
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

    doThrow(new RuntimeException("errorMessage")).when(blockchainProcessor).fullReset();

    final JSONObject result = ((JSONObject) t.processRequest(req));

    assertEquals("java.lang.RuntimeException: errorMessage", result.get(ERROR_RESPONSE));
  }

  @Test
  public void requirePost() {
    assertTrue(t.requirePost());
  }
}