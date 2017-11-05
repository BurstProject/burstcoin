package brs.http;

import brs.Escrow;
import brs.BurstException;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetEscrowTransaction extends APIServlet.APIRequestHandler {
	
  static final GetEscrowTransaction instance = new GetEscrowTransaction();
	
  private GetEscrowTransaction() {
    super(new APITag[] {APITag.ACCOUNTS}, "escrow");
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
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
		
    return JSONData.escrowTransaction(escrow);
  }
}
