package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SetRewardRecipient extends CreateTransaction {
	
	static final SetRewardRecipient instance = new SetRewardRecipient();
	
	private SetRewardRecipient() {
		super(new APITag[] {APITag.ACCOUNTS, APITag.MINING, APITag.CREATE_TRANSACTION}, "recipient");
	}
	
	@Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		Account account = ParameterParser.getSenderAccount(req);
		Long recipient = ParameterParser.getRecipientId(req);
		Account recipientAccount = Account.getAccount(recipient);
		if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", "recipient account does not have public key");
            return response;
        }
		Attachment attachment = new Attachment.BurstMiningRewardRecipientAssignment();
        return createTransaction(req, account, recipient, 0, attachment);
	}

}
