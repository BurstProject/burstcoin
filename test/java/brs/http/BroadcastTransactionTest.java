package brs.http;

import static brs.http.common.Parameters.TRANSACTION_BYTES_PARAMETER;
import static brs.http.common.Parameters.TRANSACTION_JSON_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.FULL_HASH_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import brs.BurstException;
import brs.Transaction;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import brs.services.TransactionService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BroadcastTransactionTest {

  private BroadcastTransaction t;

  private TransactionProcessor transactionProcessorMock;
  private ParameterService parameterServiceMock;
  private TransactionService transactionServiceMock;

  @Before
  public void setUp() {
    this.transactionProcessorMock = mock(TransactionProcessor.class);
    this.parameterServiceMock = mock(ParameterService.class);
    this.transactionServiceMock = mock(TransactionService.class);

    t = new BroadcastTransaction(transactionProcessorMock, parameterServiceMock, transactionServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final String mockTransactionBytesParameter = "mockTransactionBytesParameter";
    final String mockTransactionJson = "mockTransactionJson";

    final String mockTransactionStringId = "mockTransactionStringId";
    final String mockTransactionFullHash = "mockTransactionFullHash";

    final HttpServletRequest req = mock(HttpServletRequest.class);
    final Transaction mockTransaction = mock(Transaction.class);

    when(mockTransaction.getStringId()).thenReturn(mockTransactionStringId);
    when(mockTransaction.getFullHash()).thenReturn(mockTransactionFullHash);

    when(req.getParameter(TRANSACTION_BYTES_PARAMETER)).thenReturn(mockTransactionBytesParameter);
    when(req.getParameter(TRANSACTION_JSON_PARAMETER)).thenReturn(mockTransactionJson);

    when(parameterServiceMock.parseTransaction(eq(mockTransactionBytesParameter), eq(mockTransactionJson))).thenReturn(mockTransaction);

    final JSONObject result = (JSONObject) t.processRequest(req);

    verify(transactionProcessorMock).broadcast(eq(mockTransaction));

    assertEquals(mockTransactionStringId, result.get(TRANSACTION_RESPONSE));
    assertEquals(mockTransactionFullHash, result.get(FULL_HASH_RESPONSE));
  }

  @Test
  public void processRequest_validationException() throws BurstException {
    final String mockTransactionBytesParameter = "mockTransactionBytesParameter";
    final String mockTransactionJson = "mockTransactionJson";

    final HttpServletRequest req = mock(HttpServletRequest.class);
    final Transaction mockTransaction = mock(Transaction.class);

    when(req.getParameter(TRANSACTION_BYTES_PARAMETER)).thenReturn(mockTransactionBytesParameter);
    when(req.getParameter(TRANSACTION_JSON_PARAMETER)).thenReturn(mockTransactionJson);

    when(parameterServiceMock.parseTransaction(eq(mockTransactionBytesParameter), eq(mockTransactionJson))).thenReturn(mockTransaction);

    Mockito.doThrow(BurstException.NotCurrentlyValidException.class).when(transactionServiceMock).validate(eq(mockTransaction));

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(4, result.get(ERROR_CODE_RESPONSE));
    assertNotNull(result.get(ERROR_DESCRIPTION_RESPONSE));
  }

  @Test
  public void requirePost() {
    assertTrue(t.requirePost());
  }
}
