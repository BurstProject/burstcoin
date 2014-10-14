package nxt.http;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.Subscription;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class SendMoneySubscription extends CreateTransaction {
	
	static final SendMoneySubscription instance = new SendMoneySubscription();
	
	private SendMoneySubscription() {
		super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION},
			  "recipient",
			  "amountNQT",
			  "frequency");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		Account sender = ParameterParser.getSenderAccount(req);
		Long recipient = ParameterParser.getRecipientId(req);
		Long amountNQT = ParameterParser.getAmountNQT(req);
		
		int frequency;
		try {
			frequency = Integer.parseInt(req.getParameter("frequency"));
		}
		catch(Exception e) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Invalid or missing frequency parameter");
			return response;
		}
		
		if(frequency < Constants.BURST_SUBSCRIPTION_MIN_FREQ ||
		   frequency > Constants.BURST_SUBSCRIPTION_MAX_FREQ) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Invalid frequency amount");
			return response;
		}
		
		Attachment.AdvancedPaymentSubscriptionSubscribe attachment = new Attachment.AdvancedPaymentSubscriptionSubscribe(frequency);
		
		return createTransaction(req, sender, recipient, amountNQT, attachment);
	}
}
