package brs.http;

import static brs.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;

import brs.Account;
import brs.Alias;
import brs.Attachment;
import brs.BurstException;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;


public final class BuyAlias extends CreateTransaction {

  static final BuyAlias instance = new BuyAlias();

//TODO Should this not also contain AMOUNT_NQT?                                                                              V
  private BuyAlias() {
    super(new APITag[]{APITag.ALIASES, APITag.CREATE_TRANSACTION}, ALIAS_PARAMETER, ALIAS_NAME_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account buyer = ParameterParser.getSenderAccount(req);
    Alias alias = ParameterParser.getAlias(req);
    long amountNQT = ParameterParser.getAmountNQT(req);
    if (Alias.getOffer(alias) == null) {
      return INCORRECT_ALIAS_NOTFORSALE;
    }
    long sellerId = alias.getAccountId();
    Attachment attachment = new Attachment.MessagingAliasBuy(alias.getAliasName());
    return createTransaction(req, buyer, sellerId, amountNQT, attachment);
  }
}
