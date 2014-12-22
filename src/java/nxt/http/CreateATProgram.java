package nxt.http;

import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_NAME;
import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_DESCRIPTION;
import static nxt.http.JSONResponses.INCORRECT_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH;
import static nxt.http.JSONResponses.MISSING_NAME;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.at.AT_Constants;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class CreateATProgram extends CreateTransaction {
	static final CreateATProgram instance = new CreateATProgram();
	
	private CreateATProgram() {
		super (new APITag[] {APITag.AT, APITag.CREATE_TRANSACTION}, "name", "description", "creationBytes", "code", "data", "dpages", "cspages", "uspages", "minActivationAmountNQT");
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
        
        byte[] creationBytes = null;
        
        if(req.getParameter("code") != null) {
        	try {
        		String code = req.getParameter("code");
        		if((code.length() & 1) != 0)
        			throw new IllegalArgumentException();
        		
        		String data = req.getParameter("data");
        		if(data == null)
        			data = "";
        		if((data.length() & 1) != 0)
        			throw new IllegalArgumentException();
        		
        		int cpages = (code.length() / 2 / 256) + (((code.length() / 2) % 256) != 0 ? 1 : 0);
        		int dpages = Integer.parseInt(req.getParameter("dpages"));
        		int cspages = Integer.parseInt(req.getParameter("cspages"));
        		int uspages = Integer.parseInt(req.getParameter("uspages"));
        		
        		if(dpages < 0 || cspages < 0 || uspages < 0)
        			throw new IllegalArgumentException();
        		
        		long minActivationAmount = Convert.parseUnsignedLong(req.getParameter("minActivationAmountNQT"));
        		
        		int creationLength = 4; // version + reserved
        		creationLength += 8; // pages
        		creationLength += 8; // minActivationAmount
        		creationLength += cpages * 256 <= 256 ? 1 : (cpages * 256 <= 32767 ? 2 : 4); // code size
        		creationLength += code.length() / 2;
        		creationLength += dpages * 256 <= 256 ? 1 : (dpages * 256 <= 32767 ? 2 : 4); // data size
        		creationLength += data.length() / 2;
        		
        		ByteBuffer creation = ByteBuffer.allocate(creationLength);
        		creation.order(ByteOrder.LITTLE_ENDIAN);
        		creation.putShort(AT_Constants.getInstance().AT_VERSION(Nxt.getBlockchain().getHeight()));
        		creation.putShort((short)0);
        		creation.putShort((short)cpages);
        		creation.putShort((short)dpages);
        		creation.putShort((short)cspages);
        		creation.putShort((short)uspages);
        		creation.putLong(minActivationAmount);
        		if(cpages * 256 <= 256)
        			creation.put((byte)(code.length()/2));
        		else if(cpages * 256 <= 32767)
        			creation.putShort((short)(code.length()/2));
        		else
        			creation.putInt(code.length()/2);
        		byte[] codeBytes = Convert.parseHexString(code);
        		if(codeBytes != null)
        			creation.put(codeBytes);
        		if(dpages * 256 <= 256)
        			creation.put((byte)(data.length()/2));
        		else if(dpages * 256 <= 32767)
        			creation.putShort((short)(data.length()/2));
        		else
        			creation.putInt(data.length()/2);
        		byte[] dataBytes = Convert.parseHexString(data);
        		if(dataBytes != null)
        			creation.put(dataBytes);
        		
        		creationBytes = creation.array();
        	}
        	catch(Exception e) {
        		e.printStackTrace(System.out);
        		JSONObject response = new JSONObject();
        		response.put("errorCode", 5);
    			response.put("errorDescription", "Invalid or not specified parameters");
    			return response;
        	}
        }
        
        if(creationBytes == null)
        	creationBytes = ParameterParser.getCreationBytes( req );
        
        Account account = ParameterParser.getSenderAccount(req);
		Attachment attachment = new Attachment.AutomatedTransactionsCreation( name, description, creationBytes );
		
		System.out.println("AT "+ name +" added succesfully ..");
		System.out.println();
		System.out.println();
		return createTransaction(req,account,attachment);
	}
	
}
