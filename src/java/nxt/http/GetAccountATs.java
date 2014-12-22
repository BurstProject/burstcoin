package nxt.http;

import java.util.List;

import nxt.Account;
import nxt.AT;
import nxt.NxtException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountATs extends APIServlet.APIRequestHandler {
	
	static GetAccountATs instance = new GetAccountATs();
	
	private GetAccountATs() {
		super(new APITag[] {APITag.AT, APITag.ACCOUNTS}, "account");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		
		Account account = ParameterParser.getAccount(req);
		
		List<Long> atIds = AT.getATsIssuedBy(account.getId());
		JSONArray ats = new JSONArray();
		for(long atId : atIds) {
			ats.add(JSONData.at(AT.getAT(atId)));
		}
		
		JSONObject response = new JSONObject();
        response.put("ats", ats);
        return response;
	}
}
