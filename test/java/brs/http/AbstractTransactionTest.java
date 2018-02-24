package brs.http;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Attachment;
import brs.Burst;
import brs.BurstException;
import brs.Transaction;
import brs.Transaction.Builder;
import brs.TransactionProcessor;
import brs.TransactionType.DigitalGoods;
import brs.common.AbstractUnitTest;
import brs.common.TestConstants;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;
import org.mockito.ArgumentCaptor;

public abstract class AbstractTransactionTest extends AbstractUnitTest {

  @FunctionalInterface
  public interface TransactionCreationFunction<R> {
    R apply() throws BurstException;
  }

  protected Attachment attachmentCreatedTransaction(TransactionCreationFunction r, APITransactionManager apiTransactionManagerMock) throws BurstException {
    final ArgumentCaptor<Attachment> ac = ArgumentCaptor.forClass(Attachment.class);

    when(apiTransactionManagerMock.createTransaction(any(HttpServletRequest.class), any(Account.class), any(Long.class), anyLong(), ac.capture(), anyLong())).thenReturn(mock(JSONStreamAware.class));

    r.apply();

    return ac.getValue();
  }

}
