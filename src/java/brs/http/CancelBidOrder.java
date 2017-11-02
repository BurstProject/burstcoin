package brs.http;

import brs.Account;
import brs.Attachment;
import brs.BurstException;
import brs.Order;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;

public final class CancelBidOrder extends CreateTransaction {

    static final CancelBidOrder instance = new CancelBidOrder();

    private CancelBidOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "order");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
        long orderId = ParameterParser.getOrderId(req);
        Account account = ParameterParser.getSenderAccount(req);
        Order.Bid orderData = Order.Bid.getBidOrder(orderId);
        if (orderData == null || orderData.getAccountId() != account.getId()) {
            return UNKNOWN_ORDER;
        }
        Attachment attachment = new Attachment.ColoredCoinsBidOrderCancellation(orderId);
        return createTransaction(req, account, attachment);
    }

}
