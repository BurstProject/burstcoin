package brs.http;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Alias;
import brs.Alias.Offer;
import brs.Attachment;
import brs.Burst;
import brs.BurstException;
import brs.BurstException.NotValidException;
import brs.Transaction;
import brs.Transaction.Builder;
import brs.TransactionProcessor;
import brs.TransactionType.DigitalGoods;
import brs.common.TestConstants;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;

public class AbstractTransactionTest {

  public void prepareTransactionTest(HttpServletRequest req, ParameterService parameterServiceMock, TransactionProcessor transactionProcessorMock, AliasService aliasService) throws BurstException {
    final long mockSellerId = 123L;
    final String mockAliasName = "mockAliasName";

    final Account mockBuyerAccount = mock(Account.class);

    final Alias mockAlias = mock(Alias.class);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockBuyerAccount);
    when(mockAlias.getAccountId()).thenReturn(mockSellerId);
    when(mockAlias.getAliasName()).thenReturn(mockAliasName);
    when(mockBuyerAccount.getUnconfirmedBalanceNQT()).thenReturn(TestConstants.TEN_BURST);

    Builder mockBuilder = mock(Builder.class);
    when(mockBuilder.referencedTransactionFullHash(anyString())).thenReturn(mockBuilder);
    when(transactionProcessorMock.newTransactionBuilder(any(byte[].class), anyLong(), anyLong(), anyShort(), any(Attachment.class))).thenReturn(mockBuilder);

    final Offer mockOfferOnAlias = mock(Offer.class);

    when(aliasService.getOffer(eq(mockAlias))).thenReturn(mockOfferOnAlias);

    final Transaction transaction = mock(Transaction.class);
    when(mockBuilder.build()).thenReturn(transaction);
    when(transaction.getSignature()).thenReturn(new byte[5]);
    when(transaction.getType()).thenReturn(DigitalGoods.DELISTING);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockBuyerAccount);
    when(parameterServiceMock.getAlias(eq(req))).thenReturn(mockAlias);
  }
}
