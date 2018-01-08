package brs.http;

import static brs.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;

import brs.Account;
import brs.Alias;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;


public final class BuyAlias extends CreateTransaction {

  private final ParameterService parameterService;

  public BuyAlias(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain) {
    //TODO Should this not also contain AMOUNT_NQT?                                                      V
    super(new APITag[]{APITag.ALIASES, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, ALIAS_PARAMETER, ALIAS_NAME_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account buyer = parameterService.getSenderAccount(req);
    Alias alias = parameterService.getAlias(req);
    long amountNQT = parameterService.getAmountNQT(req);
    if (Alias.getOffer(alias) == null) {
      return INCORRECT_ALIAS_NOTFORSALE;
    }
    long sellerId = alias.getAccountId();
    Attachment attachment = new Attachment.MessagingAliasBuy(alias.getAliasName());
    return createTransaction(req, buyer, sellerId, amountNQT, attachment);
  }
}
