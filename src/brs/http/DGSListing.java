package brs.http;

import static brs.http.JSONResponses.INCORRECT_DGS_LISTING_DESCRIPTION;
import static brs.http.JSONResponses.INCORRECT_DGS_LISTING_NAME;
import static brs.http.JSONResponses.INCORRECT_DGS_LISTING_TAGS;
import static brs.http.JSONResponses.MISSING_NAME;
import static brs.http.common.Parameters.DESCRIPTION_PARAMETER;
import static brs.http.common.Parameters.NAME_PARAMETER;
import static brs.http.common.Parameters.PRICE_NQT_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_PARAMETER;
import static brs.http.common.Parameters.TAGS_PARAMETER;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Constants;
import brs.TransactionProcessor;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class DGSListing extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  DGSListing(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain, AccountService accountService, TransactionService transactionService) {
    super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, accountService, transactionService,
        NAME_PARAMETER, DESCRIPTION_PARAMETER, TAGS_PARAMETER, QUANTITY_PARAMETER, PRICE_NQT_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String name = Convert.emptyToNull(req.getParameter(NAME_PARAMETER));
    String description = Convert.nullToEmpty(req.getParameter(DESCRIPTION_PARAMETER));
    String tags = Convert.nullToEmpty(req.getParameter(TAGS_PARAMETER));
    long priceNQT = ParameterParser.getPriceNQT(req);
    int quantity = ParameterParser.getGoodsQuantity(req);

    if (name == null) {
      return MISSING_NAME;
    }
    name = name.trim();
    if (name.length() > Constants.MAX_DGS_LISTING_NAME_LENGTH) {
      return INCORRECT_DGS_LISTING_NAME;
    }

    if (description.length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH) {
      return INCORRECT_DGS_LISTING_DESCRIPTION;
    }

    if (tags.length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH) {
      return INCORRECT_DGS_LISTING_TAGS;
    }

    Account account = parameterService.getSenderAccount(req);
    Attachment attachment = new Attachment.DigitalGoodsListing(name, description, tags, quantity, priceNQT, blockchain.getHeight());
    return createTransaction(req, account, attachment);

  }

}
