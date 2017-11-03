package brs.http;

import brs.Account;
import brs.Escrow;
import brs.BurstException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public final class GetAccountEscrowTransactions extends APIServlet.APIRequestHandler {
	
	static final GetAccountEscrowTransactions instance = new GetAccountEscrowTransactions();
	
	private GetAccountEscrowTransactions() {
		super(new APITag[] {APITag.ACCOUNTS}, "account");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
		
		Account account = ParameterParser.getAccount(req);
		
		Collection<Escrow> accountEscrows = Escrow.getEscrowTransactionsByParticipent(account.getId());
		
		JSONObject response = new JSONObject();
		
		JSONArray escrows = new JSONArray();
		
		for(Escrow escrow : accountEscrows) {
			escrows.add(JSONData.escrowTransaction(escrow));
		}
		
		response.put("escrows", escrows);
		return response;
	}
}
