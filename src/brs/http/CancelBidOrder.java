package brs.http;

import brs.Account;
import brs.Attachment;
import brs.BurstException;
import brs.Order;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;

public final class CancelBidOrder extends CreateTransaction {

  private final ParameterService parameterService;

  public CancelBidOrder(ParameterService parameterService, TransactionProcessor transactionProcessor) {
    super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, "order");
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Account account = parameterService.getSenderAccount(req);
    Order.Bid orderData = Order.Bid.getBidOrder(orderId);
    if (orderData == null || orderData.getAccountId() != account.getId()) {
      return UNKNOWN_ORDER;
    }
    Attachment attachment = new Attachment.ColoredCoinsBidOrderCancellation(orderId);
    return createTransaction(req, account, attachment);
  }

}
