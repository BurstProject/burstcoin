package brs.http;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;

import brs.Account;
import brs.Attachment;
import brs.BurstException;
import brs.Order;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class CancelAskOrder extends CreateTransaction {

  static final CancelAskOrder instance = new CancelAskOrder();

  private CancelAskOrder() {
    super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, ORDER_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Account account = ParameterParser.getSenderAccount(req);
    Order.Ask orderData = Order.Ask.getAskOrder(orderId);
    if (orderData == null || orderData.getAccountId() != account.getId()) {
      return UNKNOWN_ORDER;
    }
    Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(orderId);
    return createTransaction(req, account, attachment);
  }

}
