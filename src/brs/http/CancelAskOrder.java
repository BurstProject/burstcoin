package brs.http;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;

import brs.Account;
import brs.Attachment;
import brs.BurstException;
import brs.Order;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class CancelAskOrder extends CreateTransaction {

  private final ParameterService parameterService;

  public CancelAskOrder(ParameterService parameterService, TransactionProcessor transactionProcessor) {
    super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, ORDER_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Account account = parameterService.getSenderAccount(req);
    Order.Ask orderData = Order.Ask.getAskOrder(orderId);
    if (orderData == null || orderData.getAccountId() != account.getId()) {
      return UNKNOWN_ORDER;
    }
    Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(orderId);
    return createTransaction(req, account, attachment);
  }

}
