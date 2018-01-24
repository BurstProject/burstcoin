package brs.http;

import static brs.http.JSONResponses.ALREADY_DELIVERED;
import static brs.http.JSONResponses.INCORRECT_DGS_DISCOUNT;
import static brs.http.JSONResponses.INCORRECT_DGS_GOODS;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;
import static brs.http.common.Parameters.DISCOUNT_NQT_PARAMETER;
import static brs.http.common.Parameters.GOODS_DATA_PARAMETER;
import static brs.http.common.Parameters.GOODS_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.GOODS_NONCE_PARAMETER;
import static brs.http.common.Parameters.GOODS_TO_ENCRYPT_PARAMETER;
import static brs.http.common.Parameters.PURCHASE_PARAMETER;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Constants;
import brs.DigitalGoodsStore;
import brs.TransactionProcessor;
import brs.crypto.EncryptedData;
import brs.http.common.Parameters;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class DGSDelivery extends CreateTransaction {

  private final ParameterService parameterService;
  private final AccountService accountService;
  private final Blockchain blockchain;

  DGSDelivery(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain, AccountService accountService) {
    super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, accountService,
        PURCHASE_PARAMETER, DISCOUNT_NQT_PARAMETER, GOODS_TO_ENCRYPT_PARAMETER, GOODS_IS_TEXT_PARAMETER, GOODS_DATA_PARAMETER, GOODS_NONCE_PARAMETER);
    this.parameterService = parameterService;
    this.accountService = accountService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account sellerAccount = parameterService.getSenderAccount(req);
    DigitalGoodsStore.Purchase purchase = parameterService.getPurchase(req);
    if (sellerAccount.getId() != purchase.getSellerId()) {
      return INCORRECT_PURCHASE;
    }
    if (!purchase.isPending()) {
      return ALREADY_DELIVERED;
    }

    String discountValueNQT = Convert.emptyToNull(req.getParameter(DISCOUNT_NQT_PARAMETER));
    long discountNQT = 0;
    try {
      if (discountValueNQT != null) {
        discountNQT = Long.parseLong(discountValueNQT);
      }
    } catch (RuntimeException e) {
      return INCORRECT_DGS_DISCOUNT;
    }
    if (discountNQT < 0
        || discountNQT > Constants.MAX_BALANCE_NQT
        || discountNQT > Convert.safeMultiply(purchase.getPriceNQT(), purchase.getQuantity())) {
      return INCORRECT_DGS_DISCOUNT;
    }

    Account buyerAccount = accountService.getAccount(purchase.getBuyerId());
    boolean goodsIsText = Parameters.isFalse(req.getParameter(GOODS_IS_TEXT_PARAMETER));
    EncryptedData encryptedGoods = ParameterParser.getEncryptedGoods(req);

    if (encryptedGoods == null) {
      String secretPhrase = ParameterParser.getSecretPhrase(req);
      byte[] goodsBytes;
      try {
        String plainGoods = Convert.nullToEmpty(req.getParameter(GOODS_TO_ENCRYPT_PARAMETER));
        if (plainGoods.isEmpty()) {
          return INCORRECT_DGS_GOODS;
        }
        goodsBytes = goodsIsText ? Convert.toBytes(plainGoods) : Convert.parseHexString(plainGoods);
      } catch (RuntimeException e) {
        return INCORRECT_DGS_GOODS;
      }
      encryptedGoods = buyerAccount.encryptTo(goodsBytes, secretPhrase);
    }

    Attachment attachment = new Attachment.DigitalGoodsDelivery(purchase.getId(), encryptedGoods, goodsIsText, discountNQT, blockchain.getHeight());
    return createTransaction(req, sellerAccount, buyerAccount.getId(), 0, attachment);

  }

}
