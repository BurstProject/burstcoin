package brs.http;

import static brs.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static brs.http.JSONResponses.INCORRECT_ASSET_NAME;
import static brs.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static brs.http.JSONResponses.INCORRECT_DECIMALS;
import static brs.http.JSONResponses.MISSING_NAME;
import static brs.http.common.Parameters.DECIMALS_PARAMETER;
import static brs.http.common.Parameters.DESCRIPTION_PARAMETER;
import static brs.http.common.Parameters.NAME_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_NQT_PARAMETER;

import brs.Account;
import brs.AssetTransfer;
import brs.Attachment;
import brs.Block;
import brs.Blockchain;
import brs.BurstException;
import brs.Constants;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class IssueAsset extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  IssueAsset(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain) {
    super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, NAME_PARAMETER, DESCRIPTION_PARAMETER, QUANTITY_NQT_PARAMETER, DECIMALS_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String name = req.getParameter("name");
    String description = req.getParameter("description");
    String decimalsValue = Convert.emptyToNull(req.getParameter("decimals"));

    if (name == null) {
      return MISSING_NAME;
    }

    name = name.trim();
    if (name.length() < Constants.MIN_ASSET_NAME_LENGTH || name.length() > Constants.MAX_ASSET_NAME_LENGTH) {
      return INCORRECT_ASSET_NAME_LENGTH;
    }
    String normalizedName = name.toLowerCase();
    for (int i = 0; i < normalizedName.length(); i++) {
      if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
        return INCORRECT_ASSET_NAME;
      }
    }

    if (description != null && description.length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH) {
      return INCORRECT_ASSET_DESCRIPTION;
    }

    byte decimals = 0;
    if (decimalsValue != null) {
      try {
        decimals = Byte.parseByte(decimalsValue);
        if (decimals < 0 || decimals > 8) {
          return INCORRECT_DECIMALS;
        }
      } catch (NumberFormatException e) {
        return INCORRECT_DECIMALS;
      }
    }

    long quantityQNT = ParameterParser.getQuantityQNT(req);
    Account account = parameterService.getSenderAccount(req);
    Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantityQNT, decimals, blockchain.getHeight());
    return createTransaction(req, account, attachment);

  }

}
