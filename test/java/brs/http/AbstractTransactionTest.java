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
import brs.BurstException;
import brs.Transaction;
import brs.Transaction.Builder;
import brs.TransactionProcessor;
import brs.TransactionType.DigitalGoods;
import brs.common.AbstractUnitTest;
import brs.common.TestConstants;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;

public abstract class AbstractTransactionTest extends AbstractUnitTest {

  public void prepareTransactionTest(HttpServletRequest req, ParameterService parameterServiceMock, TransactionProcessor transactionProcessorMock) throws BurstException {
    Account sellerAccount = mock(Account.class);
    when(sellerAccount.getUnconfirmedBalanceNQT()).thenReturn(TestConstants.TEN_BURST);
    prepareTransactionTest(req, parameterServiceMock, transactionProcessorMock, sellerAccount);
  }

  public void prepareTransactionTest(HttpServletRequest req, ParameterService parameterServiceMock, TransactionProcessor transactionProcessorMock, Account senderAccount) throws BurstException {
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(senderAccount);

    Builder mockBuilder = mock(Builder.class);
    when(mockBuilder.referencedTransactionFullHash(anyString())).thenReturn(mockBuilder);
    when(transactionProcessorMock.newTransactionBuilder(any(byte[].class), anyLong(), anyLong(), anyShort(), any(Attachment.class))).thenReturn(mockBuilder);

    final Transaction transaction = mock(Transaction.class);
    when(mockBuilder.build()).thenReturn(transaction);
    when(transaction.getSignature()).thenReturn(new byte[5]);
    when(transaction.getType()).thenReturn(DigitalGoods.DELISTING);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(senderAccount);
  }

}
