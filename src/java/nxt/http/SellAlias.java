package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ALIAS_OWNER;
import static nxt.http.JSONResponses.INCORRECT_PRICE;
import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_PRICE;


public final class SellAlias extends CreateTransaction {

    static final SellAlias instance = new SellAlias();

    private SellAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "alias", "aliasName", "recipient", "priceNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Alias alias = ParameterParser.getAlias(req);
        Account owner = ParameterParser.getSenderAccount(req);

        String priceValueNQT = Convert.emptyToNull(req.getParameter("priceNQT"));
        if (priceValueNQT == null) {
            return MISSING_PRICE;
        }
        long priceNQT;
        try {
            priceNQT = Long.parseLong(priceValueNQT);
        } catch (RuntimeException e) {
            return INCORRECT_PRICE;
        }
        if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(INCORRECT_PRICE);
        }

        String recipientValue = Convert.emptyToNull(req.getParameter("recipient"));
        Long recipientId = null;
        if (recipientValue != null) {
            try {
                recipientId = Convert.parseAccountId(recipientValue);
            } catch (RuntimeException e) {
                return INCORRECT_RECIPIENT;
            }
            if (recipientId == null) {
                return INCORRECT_RECIPIENT;
            }
        }

        if (! alias.getAccountId().equals(owner.getId())) {
            return INCORRECT_ALIAS_OWNER;
        }

        Attachment attachment = new Attachment.MessagingAliasSell(alias.getAliasName(), priceNQT);
        return createTransaction(req, owner, recipientId, 0, attachment);
    }
}
