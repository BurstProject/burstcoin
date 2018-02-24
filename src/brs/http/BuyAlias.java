package brs.http;

import static brs.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;

import brs.Account;
import brs.Alias;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class BuyAlias extends CreateTransaction {

  private final ParameterService parameterService;
  private final AliasService aliasService;
  private final Blockchain blockchain;

  public BuyAlias(ParameterService parameterService, Blockchain blockchain, AliasService aliasService, APITransactionManager apiTransactionManager) {
    super(new APITag[]{APITag.ALIASES, APITag.CREATE_TRANSACTION}, apiTransactionManager, ALIAS_PARAMETER, ALIAS_NAME_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
    this.aliasService = aliasService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account buyer = parameterService.getSenderAccount(req);
    Alias alias = parameterService.getAlias(req);
    long amountNQT = ParameterParser.getAmountNQT(req);

    if (aliasService.getOffer(alias) == null) {
      return INCORRECT_ALIAS_NOTFORSALE;
    }

    long sellerId = alias.getAccountId();
    Attachment attachment = new Attachment.MessagingAliasBuy(alias.getAliasName(), blockchain.getHeight());
    return createTransaction(req, buyer, sellerId, amountNQT, attachment);
  }
}
