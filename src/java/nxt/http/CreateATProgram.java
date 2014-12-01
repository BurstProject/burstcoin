package nxt.http;

import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_NAME;
import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_DESCRIPTION;
import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH;

import static nxt.http.JSONResponses.MISSING_NAME;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;

import org.json.simple.JSONStreamAware;

public final class CreateATProgram extends CreateTransaction {
	static final CreateATProgram instance = new CreateATProgram();
	
	private CreateATProgram() {
		super (new APITag[] {APITag.AT, APITag.CREATE_TRANSACTION}, "name", "description", "creationBytes");
	}
	
	@Override
	JSONStreamAware processRequest (HttpServletRequest req) throws NxtException {
		//String atVersion = req.getParameter("atVersion");		
		String name = req.getParameter("name");
		String description = req.getParameter("description");
		
		if (name == null) {
            return MISSING_NAME;
        }

        name = name.trim();
        if (name.length() > Constants.MAX_AUTOMATED_TRANSACTION_NAME_LENGTH) {
            return INCORRECT_AUTOMATED_TRANSACTION_NAME_LENGTH;
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return INCORRECT_AUTOMATED_TRANSACTION_NAME;
            }
        }

        if (description != null && description.length() > Constants.MAX_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH) {
            return INCORRECT_AUTOMATED_TRANSACTION_DESCRIPTION;
        }
        
        byte[] creationBytes = ParameterParser.getCreationBytes( req );
        
        Account account = ParameterParser.getSenderAccount(req);
		Attachment attachment = new Attachment.AutomatedTransactionsCreation( name, description, creationBytes );
		
		System.out.println("AT "+ name +" added succesfully ..");
		System.out.println();
		System.out.println();
		return createTransaction(req,account,attachment);
	}
	
}
