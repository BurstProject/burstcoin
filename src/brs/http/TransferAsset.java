package brs.http;

import brs.Account;
import brs.Asset;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.NOT_ENOUGH_ASSETS;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_NQT_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;

public final class TransferAsset extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  public TransferAsset(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain, AccountService accountService, TransactionService transactionService) {
    super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, accountService, transactionService, RECIPIENT_PARAMETER, ASSET_PARAMETER, QUANTITY_NQT_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long recipient = ParameterParser.getRecipientId(req);

    Asset asset = parameterService.getAsset(req);
    long quantityQNT = ParameterParser.getQuantityQNT(req);
    Account account = parameterService.getSenderAccount(req);

    long assetBalance = account.getUnconfirmedAssetBalanceQNT(asset.getId());
    if (assetBalance < 0 || quantityQNT > assetBalance) {
      return NOT_ENOUGH_ASSETS;
    }

    Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset.getId(), quantityQNT, blockchain.getHeight());
    return createTransaction(req, account, recipient, 0, attachment);

  }

}
