package brs.http;

import brs.Account;
import brs.Attachment;
import brs.DigitalGoodsStore;
import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.GOODS_NOT_DELIVERED;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSFeedback extends CreateTransaction {

  static final DGSFeedback instance = new DGSFeedback();

  private DGSFeedback() {
    super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
          "purchase");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);

    Account buyerAccount = ParameterParser.getSenderAccount(req);
    if (buyerAccount.getId() != purchase.getBuyerId()) {
      return INCORRECT_PURCHASE;
    }
    if (purchase.getEncryptedGoods() == null) {
      return GOODS_NOT_DELIVERED;
    }

    Account sellerAccount = Account.getAccount(purchase.getSellerId());
    Attachment attachment = new Attachment.DigitalGoodsFeedback(purchase.getId());
    return createTransaction(req, buyerAccount, sellerAccount.getId(), 0, attachment);
  }

}
