package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetRewardRecipient extends APIServlet.APIRequestHandler {
	
	static final GetRewardRecipient instance = new GetRewardRecipient();
	
	private GetRewardRecipient() {
		super(new APITag[] {APITag.ACCOUNTS, APITag.MINING, APITag.INFO});
	}
	
	@Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		JSONObject response = new JSONObject();
		
		Account account = ParameterParser.getAccount(req);
		long height = Nxt.getBlockchain().getLastBlock().getHeight();
		if(account.getRewardRecipientFrom() > height + 1) {
			response.put("rewardRecipient", Convert.toUnsignedLong(account.getPrevRewardRecipient()));
		}
		else {
			response.put("rewardRecipient", Convert.toUnsignedLong(account.getRewardRecipient()));
		}
		
		return response;
	}

}
