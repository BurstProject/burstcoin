package brs.http;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Order;
import brs.assetexchange.AssetExchange;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;

public final class CancelBidOrder extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;
  private final AssetExchange assetExchange;

  public CancelBidOrder(ParameterService parameterService, Blockchain blockchain, AssetExchange assetExchange, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, apiTransactionManager, ORDER_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
    this.assetExchange = assetExchange;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Account account = parameterService.getSenderAccount(req);
    Order.Bid orderData = assetExchange.getBidOrder(orderId);
    if (orderData == null || orderData.getAccountId() != account.getId()) {
      return UNKNOWN_ORDER;
    }
    Attachment attachment = new Attachment.ColoredCoinsBidOrderCancellation(orderId, blockchain.getHeight());
    return createTransaction(req, account, attachment);
  }

}
