package brs.http;

import brs.Account;
import brs.Asset;
import brs.Attachment;
import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.NOT_ENOUGH_ASSETS;

public final class TransferAsset extends CreateTransaction {

    static final TransferAsset instance = new TransferAsset();

    private TransferAsset() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "recipient", "asset", "quantityQNT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

        long recipient = ParameterParser.getRecipientId(req);

        Asset asset = ParameterParser.getAsset(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);

        long assetBalance = account.getUnconfirmedAssetBalanceQNT(asset.getId());
        if (assetBalance < 0 || quantityQNT > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset.getId(), quantityQNT);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
