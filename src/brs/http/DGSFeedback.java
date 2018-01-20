package brs.http;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.DigitalGoodsStore;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.services.AccountService;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.GOODS_NOT_DELIVERED;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;
import static brs.http.common.Parameters.PURCHASE_PARAMETER;

public final class DGSFeedback extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  DGSFeedback(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain, AccountService accountService) {
    super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
        parameterService, transactionProcessor, blockchain, accountService, PURCHASE_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    DigitalGoodsStore.Purchase purchase = parameterService.getPurchase(req);

    Account buyerAccount = parameterService.getSenderAccount(req);
    if (buyerAccount.getId() != purchase.getBuyerId()) {
      return INCORRECT_PURCHASE;
    }
    if (purchase.getEncryptedGoods() == null) {
      return GOODS_NOT_DELIVERED;
    }

    Account sellerAccount = Account.getAccount(purchase.getSellerId());
    Attachment attachment = new Attachment.DigitalGoodsFeedback(purchase.getId(), blockchain.getHeight());
    return createTransaction(req, buyerAccount, sellerAccount.getId(), 0, attachment);
  }

}
