package nxt.http;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Escrow;
import nxt.NxtException;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class SendMoneyEscrow extends CreateTransaction {
	
	static final SendMoneyEscrow instance = new SendMoneyEscrow();
	
	private SendMoneyEscrow() {
		super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION},
			  "recipient",
			  "amountNQT",
			  "escrowDeadline",
			  "deadlineAction",
			  "requiredSigners",
			  "signers");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		Account sender = ParameterParser.getSenderAccount(req);
		Long recipient = ParameterParser.getRecipientId(req);
		Long amountNQT = ParameterParser.getAmountNQT(req);
		String signerString = Convert.emptyToNull(req.getParameter("signers"));
		
		Long requiredSigners;
		try {
			requiredSigners = Convert.parseLong(req.getParameter("requiredSigners"));
			if(requiredSigners < 1 || requiredSigners > 10) {
				JSONObject response = new JSONObject();
				response.put("errorCode", 4);
				response.put("errorDescription", "Invalid number of requiredSigners");
				return response;
			}
		}
		catch(Exception e) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Invalid requiredSigners parameter");
			return response;
		}
		
		if(signerString == null) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 3);
			response.put("errorDescription", "Signers not specified");
			return response;
		}
		
		String signersArray[] = signerString.split(";", 10);
		
		if(signersArray.length < 1 || signersArray.length > 10 || signersArray.length < requiredSigners) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Invalid number of signers");
			return response;
		}
		
		ArrayList<Long> signers = new ArrayList<>();
		
		try {
			for(String signer : signersArray) {
				Long id = Convert.parseAccountId(signer);
				if(id == null) {
					throw new Exception("");
				}
				
				signers.add(id);
			}
		}
		catch(Exception e) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Invalid signers parameter");
			return response;
		}
		
		Long totalAmountNQT = Convert.safeAdd(amountNQT, signers.size() * Constants.ONE_NXT);
		if(sender.getBalanceNQT() < totalAmountNQT) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 6);
			response.put("errorDescription", "Insufficient funds");
			return response;
		}
		
		Long deadline;
		try {
			deadline = Convert.parseLong(req.getParameter("escrowDeadline"));
			if(deadline < 1 || deadline > 7776000) {
				JSONObject response = new JSONObject();
				response.put("errorCode", 4);
				response.put("errorDescription", "Escrow deadline must be 1 - 7776000");
				return response;
			}
		}
		catch(Exception e) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Invalid escrowDeadline parameter");
			return response;
		}
		
		Escrow.DecisionType deadlineAction = Escrow.stringToDecision(req.getParameter("deadlineAction"));
		if(deadlineAction == null || deadlineAction == Escrow.DecisionType.UNDECIDED) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Invalid deadlineAction parameter");
			return response;
		}
		
		Attachment.AdvancedPaymentEscrowCreation attachment = new Attachment.AdvancedPaymentEscrowCreation(amountNQT, deadline.intValue(), deadlineAction, requiredSigners.intValue(), signers);
		
		return createTransaction(req, sender, recipient, 0, attachment);
	}
}
