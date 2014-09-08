package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendMessage extends CreateTransaction {

    static final SendMessage instance = new SendMessage();

    private SendMessage() {
        super(new APITag[] {APITag.MESSAGES, APITag.CREATE_TRANSACTION}, "recipient");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Long recipient = ParameterParser.getRecipientId(req);
        Account account = ParameterParser.getSenderAccount(req);
        return createTransaction(req, account, recipient, 0, Attachment.ARBITRARY_MESSAGE);
    }

}
