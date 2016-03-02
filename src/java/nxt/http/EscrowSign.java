package nxt.http;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Escrow;
import nxt.NxtException;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class EscrowSign extends CreateTransaction {
	
	static final EscrowSign instance = new EscrowSign();
	
	private EscrowSign() {
		super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION},
			  "escrow",
			  "decision");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		Long escrowId;
		try {
			escrowId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter("escrow")));
		}
		catch(Exception e) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 3);
			response.put("errorDescription", "Invalid or not specified escrow");
			return response;
		}
		
		Escrow escrow = Escrow.getEscrowTransaction(escrowId);
		if(escrow == null) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 5);
			response.put("errorDescription", "Escrow transaction not found");
			return response;
		}
		
		Escrow.DecisionType decision = Escrow.stringToDecision(req.getParameter("decision"));
		if(decision == null) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 5);
			response.put("errorDescription", "Invalid or not specified action");
			return response;
		}
		
		Account sender = ParameterParser.getSenderAccount(req);
		if(!(escrow.getSenderId().equals(sender.getId())) &&
		   !(escrow.getRecipientId().equals(sender.getId())) &&
		   !escrow.isIdSigner(sender.getId())) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 5);
			response.put("errorDescription", "Invalid or not specified action");
			return response;
		}
		
		if(escrow.getSenderId().equals(sender.getId()) && decision != Escrow.DecisionType.RELEASE) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Sender can only release");
			return response;
		}
		
		if(escrow.getRecipientId().equals(sender.getId()) && decision != Escrow.DecisionType.REFUND) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Recipient can only refund");
			return response;
		}
		
		Attachment.AdvancedPaymentEscrowSign attachment = new Attachment.AdvancedPaymentEscrowSign(escrow.getId(), decision);
		
		return createTransaction(req, sender, null, 0, attachment);
	}
}
