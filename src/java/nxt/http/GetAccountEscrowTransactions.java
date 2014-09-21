package nxt.http;

import java.util.Collection;

import nxt.Account;
import nxt.Escrow;
import nxt.NxtException;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountEscrowTransactions extends APIServlet.APIRequestHandler {
	
	static final GetAccountEscrowTransactions instance = new GetAccountEscrowTransactions();
	
	private GetAccountEscrowTransactions() {
		super(new APITag[] {APITag.ACCOUNTS}, "account");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		
		Account account = ParameterParser.getAccount(req);
		
		Collection<Escrow> accountEscrows = Escrow.getEscrowTransactionsByParticipent(account.getId());
		
		JSONObject response = new JSONObject();
		
		JSONArray escrows = new JSONArray();
		
		for(Escrow escrow : accountEscrows) {
			JSONObject escrowDetails = new JSONObject();
			
			escrowDetails.put("id", escrow.getId());
			escrowDetails.put("sender", escrow.getSenderId());
			escrowDetails.put("senderRS", Convert.rsAccount(escrow.getSenderId()));
			escrowDetails.put("recipient", escrow.getRecipientId());
			escrowDetails.put("recipientRS", Convert.rsAccount(escrow.getRecipientId()));
			
			escrows.add(escrowDetails);
		}
		
		response.put("escrows", escrows);
		return response;
	}
}
