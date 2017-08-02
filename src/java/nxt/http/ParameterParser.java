package nxt.http;

import nxt.AT;
import nxt.Account;
import nxt.Alias;
import nxt.Asset;
import nxt.Constants;
import nxt.DigitalGoodsStore;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.LoggerConfigurator;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.*;

final class ParameterParser {

    private static final Logger logger = LoggerFactory.getLogger(ParameterParser.class);

    static Alias getAlias(HttpServletRequest req) throws ParameterException {
        long aliasId;
        try {
            aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter("alias")));
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ALIAS);
        }
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        Alias alias;
        if (aliasId != 0) {
            alias = Alias.getAlias(aliasId);
        } else if (aliasName != null) {
            alias = Alias.getAlias(aliasName);
        } else {
            throw new ParameterException(MISSING_ALIAS_OR_ALIAS_NAME);
        }
        if (alias == null) {
            throw new ParameterException(UNKNOWN_ALIAS);
        }
        return alias;
    }

    static long getAmountNQT(HttpServletRequest req) throws ParameterException {
        String amountValueNQT = Convert.emptyToNull(req.getParameter("amountNQT"));
        if (amountValueNQT == null) {
            throw new ParameterException(MISSING_AMOUNT);
        }
        long amountNQT;
        try {
            amountNQT = Long.parseLong(amountValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_AMOUNT);
        }
        if (amountNQT <= 0 || amountNQT >= Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(INCORRECT_AMOUNT);
        }
        return amountNQT;
    }

    static long getFeeNQT(HttpServletRequest req) throws ParameterException {
        String feeValueNQT = Convert.emptyToNull(req.getParameter("feeNQT"));
        if (feeValueNQT == null) {
            throw new ParameterException(MISSING_FEE);
        }
        long feeNQT;
        try {
            feeNQT = Long.parseLong(feeValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_FEE);
        }
        if (feeNQT < 0 || feeNQT >= Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(INCORRECT_FEE);
        }
        return feeNQT;
    }

    static long getPriceNQT(HttpServletRequest req) throws ParameterException {
        String priceValueNQT = Convert.emptyToNull(req.getParameter("priceNQT"));
        if (priceValueNQT == null) {
            throw new ParameterException(MISSING_PRICE);
        }
        long priceNQT;
        try {
            priceNQT = Long.parseLong(priceValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PRICE);
        }
        if (priceNQT <= 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(INCORRECT_PRICE);
        }
        return priceNQT;
    }

    static Asset getAsset(HttpServletRequest req) throws ParameterException {
        String assetValue = Convert.emptyToNull(req.getParameter("asset"));
        if (assetValue == null) {
            throw new ParameterException(MISSING_ASSET);
        }
        Asset asset;
        try {
            long assetId = Convert.parseUnsignedLong(assetValue);
            asset = Asset.getAsset(assetId);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ASSET);
        }
        if (asset == null) {
            throw new ParameterException(UNKNOWN_ASSET);
        }
        return asset;
    }

    static long getQuantityQNT(HttpServletRequest req) throws ParameterException {
        String quantityValueQNT = Convert.emptyToNull(req.getParameter("quantityQNT"));
        if (quantityValueQNT == null) {
            throw new ParameterException(MISSING_QUANTITY);
        }
        long quantityQNT;
        try {
            quantityQNT = Long.parseLong(quantityValueQNT);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_QUANTITY);
        }
        if (quantityQNT <= 0 || quantityQNT > Constants.MAX_ASSET_QUANTITY_QNT) {
            throw new ParameterException(INCORRECT_ASSET_QUANTITY);
        }
        return quantityQNT;
    }

    static long getOrderId(HttpServletRequest req) throws ParameterException {
        String orderValue = Convert.emptyToNull(req.getParameter("order"));
        if (orderValue == null) {
            throw new ParameterException(MISSING_ORDER);
        }
        try {
            return Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ORDER);
        }
    }

    static DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException {
        String goodsValue = Convert.emptyToNull(req.getParameter("goods"));
        if (goodsValue == null) {
            throw new ParameterException(MISSING_GOODS);
        }
        DigitalGoodsStore.Goods goods;
        try {
            long goodsId = Convert.parseUnsignedLong(goodsValue);
            goods = DigitalGoodsStore.getGoods(goodsId);
            if (goods == null) {
                throw new ParameterException(UNKNOWN_GOODS);
            }
            return goods;
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_GOODS);
        }
    }

    static int getGoodsQuantity(HttpServletRequest req) throws ParameterException {
        String quantityString = Convert.emptyToNull(req.getParameter("quantity"));
        try {
            int quantity = Integer.parseInt(quantityString);
            if (quantity < 0 || quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
                throw new ParameterException(INCORRECT_QUANTITY);
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_QUANTITY);
        }
    }

    static EncryptedData getEncryptedMessage(HttpServletRequest req, Account recipientAccount) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter("encryptedMessageData"));
        String nonce = Convert.emptyToNull(req.getParameter("encryptedMessageNonce"));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
            }
        }
        String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncrypt"));
        if (plainMessage == null) {
            return null;
        }
        if (recipientAccount == null) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        String secretPhrase = getSecretPhrase(req);
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText"));
        try {
            byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            return recipientAccount.encryptTo(plainMessageBytes, secretPhrase);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
        }
    }

    static EncryptedData getEncryptToSelfMessage(HttpServletRequest req) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter("encryptToSelfMessageData"));
        String nonce = Convert.emptyToNull(req.getParameter("encryptToSelfMessageNonce"));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
            }
        }
        String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncryptToSelf"));
        if (plainMessage == null) {
            return null;
        }
        String secretPhrase = getSecretPhrase(req);
        Account senderAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptToSelfIsText"));
        try {
            byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            return senderAccount.encryptTo(plainMessageBytes, secretPhrase);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
        }
    }

    static EncryptedData getEncryptedGoods(HttpServletRequest req) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter("goodsData"));
        String nonce = Convert.emptyToNull(req.getParameter("goodsNonce"));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_DGS_ENCRYPTED_GOODS);
            }
        }
        return null;
    }

    static DigitalGoodsStore.Purchase getPurchase(HttpServletRequest req) throws ParameterException {
        String purchaseIdString = Convert.emptyToNull(req.getParameter("purchase"));
        if (purchaseIdString == null) {
            throw new ParameterException(MISSING_PURCHASE);
        }
        try {
            DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.getPurchase(Convert.parseUnsignedLong(purchaseIdString));
            if (purchase == null) {
                throw new ParameterException(INCORRECT_PURCHASE);
            }
            return purchase;
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_PURCHASE);
        }
    }

    static String getSecretPhrase(HttpServletRequest req) throws ParameterException {
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase == null) {
            throw new ParameterException(MISSING_SECRET_PHRASE);
        }
        return secretPhrase;
    }

    static Account getSenderAccount(HttpServletRequest req) throws ParameterException {
        Account account;
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyString = Convert.emptyToNull(req.getParameter("publicKey"));
        if (secretPhrase != null) {
            account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        } else if (publicKeyString != null) {
            try {
                account = Account.getAccount(Convert.parseHexString(publicKeyString));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_PUBLIC_KEY);
            }
        } else {
            throw new ParameterException(MISSING_SECRET_PHRASE_OR_PUBLIC_KEY);
        }
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        return account;
    }

    static Account getAccount(HttpServletRequest req) throws ParameterException {
        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        if (accountValue == null) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        try {
            Account account = Account.getAccount(Convert.parseAccountId(accountValue));
            if (account == null) {
                throw new ParameterException(UNKNOWN_ACCOUNT);
            }
            return account;
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ACCOUNT);
        }
    }

    static List<Account> getAccounts(HttpServletRequest req) throws ParameterException {
        String[] accountValues = req.getParameterValues("account");
        if (accountValues == null || accountValues.length == 0) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        List<Account> result = new ArrayList<>();
        for (String accountValue : accountValues) {
            if (accountValue == null || accountValue.equals("")) {
                continue;
            }
            try {
                Account account = Account.getAccount(Convert.parseAccountId(accountValue));
                if (account == null) {
                    throw new ParameterException(UNKNOWN_ACCOUNT);
                }
                result.add(account);
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ACCOUNT);
            }
        }
        return result;
    }

    static int getTimestamp(HttpServletRequest req) throws ParameterException {
        String timestampValue = Convert.emptyToNull(req.getParameter("timestamp"));
        if (timestampValue == null) {
            return 0;
        }
        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
        } catch (NumberFormatException e) {
            throw new ParameterException(INCORRECT_TIMESTAMP);
        }
        if (timestamp < 0) {
            throw new ParameterException(INCORRECT_TIMESTAMP);
        }
        return timestamp;
    }

    static long getRecipientId(HttpServletRequest req) throws ParameterException {
        String recipientValue = Convert.emptyToNull(req.getParameter("recipient"));
        if (recipientValue == null || "0".equals(recipientValue)) {
            throw new ParameterException(MISSING_RECIPIENT);
        }
        long recipientId;
        try {
            recipientId = Convert.parseAccountId(recipientValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        if (recipientId == 0) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
        return recipientId;
    }

    static long getSellerId(HttpServletRequest req) throws ParameterException {
        String sellerIdValue = Convert.emptyToNull(req.getParameter("seller"));
        try {
            return Convert.parseAccountId(sellerIdValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
    }

    static long getBuyerId(HttpServletRequest req) throws ParameterException {
        String buyerIdValue = Convert.emptyToNull(req.getParameter("buyer"));
        try {
            return Convert.parseAccountId(buyerIdValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_RECIPIENT);
        }
    }

    static int getFirstIndex(HttpServletRequest req) {
        int firstIndex;
        try {
            firstIndex = Integer.parseInt(req.getParameter("firstIndex"));
            if (firstIndex < 0) {
                return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return firstIndex;
    }

    static int getLastIndex(HttpServletRequest req) {
        int lastIndex;
        try {
            lastIndex = Integer.parseInt(req.getParameter("lastIndex"));
            if (lastIndex < 0) {
                return Integer.MAX_VALUE;
            }
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
        return lastIndex;
    }

    static int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException {
        String numberOfConfirmationsValue = Convert.emptyToNull(req.getParameter("numberOfConfirmations"));
        if (numberOfConfirmationsValue != null) {
            try {
                int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                if (numberOfConfirmations <= Nxt.getBlockchain().getHeight()) {
                    return numberOfConfirmations;
                }
                throw new ParameterException(INCORRECT_NUMBER_OF_CONFIRMATIONS);
            } catch (NumberFormatException e) {
                throw new ParameterException(INCORRECT_NUMBER_OF_CONFIRMATIONS);
            }
        }
        return 0;
    }

    static int getHeight(HttpServletRequest req) throws ParameterException {
        String heightValue = Convert.emptyToNull(req.getParameter("height"));
        if (heightValue != null) {
            try {
                int height = Integer.parseInt(heightValue);
                if (height < 0 || height > Nxt.getBlockchain().getHeight()) {
                    throw new ParameterException(INCORRECT_HEIGHT);
                }
                if (height < Nxt.getBlockchainProcessor().getMinRollbackHeight()) {
                    throw new ParameterException(HEIGHT_NOT_AVAILABLE);
                }
                return height;
            } catch (NumberFormatException e) {
                throw new ParameterException(INCORRECT_HEIGHT);
            }
        }
        return -1;
    }

    static Transaction parseTransaction(String transactionBytes, String transactionJSON) throws ParameterException {
        if (transactionBytes == null && transactionJSON == null) {
            throw new ParameterException(MISSING_TRANSACTION_BYTES_OR_JSON);
        }
        if (transactionBytes != null) {
            try {
                byte[] bytes = Convert.parseHexString(transactionBytes);
                return Nxt.getTransactionProcessor().parseTransaction(bytes);
            } catch (NxtException.ValidationException|RuntimeException e) {
                logger.debug(e.getMessage(), e);
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect transactionBytes: " + e.toString());
                throw new ParameterException(response);
            }
        } else {
            try {
                JSONObject json = (JSONObject) JSONValue.parseWithException(transactionJSON);
                return Nxt.getTransactionProcessor().parseTransaction(json);
            } catch (NxtException.ValidationException | RuntimeException | ParseException e) {
                logger.debug(e.getMessage(), e);
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect transactionJSON: " + e.toString());
                throw new ParameterException(response);
            }
        }
    }


    private ParameterParser() {} // never
    

	
	static AT getAT(HttpServletRequest req) throws ParameterException {
        String atValue = Convert.emptyToNull(req.getParameter("at"));
        if (atValue == null) {
            throw new ParameterException(MISSING_AT);
        }
        AT at;
        try {
            Long atId = Convert.parseUnsignedLong(atValue);
            at = AT.getAT( atId );
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_AT);
        }
        if (at == null) {
            throw new ParameterException(UNKNOWN_AT);
        }
        return at;
    }
	
	public static byte[] getCreationBytes( HttpServletRequest req ) throws ParameterException {
		try {
			return Convert.parseHexString(req.getParameter("creationBytes"));
		} catch (RuntimeException e) {
			throw new ParameterException(INCORRECT_CREATION_BYTES);
		}
		
		
		
	}

	public static String getATLong(HttpServletRequest req) {
		String hex = req.getParameter("hexString");
		ByteBuffer bf = ByteBuffer.allocate(8);
		bf.order(ByteOrder.LITTLE_ENDIAN);
		bf.put(Convert.parseHexString(hex));
		
		String ret = Convert.toUnsignedLong(bf.getLong(0));
		return ret;
	}


}
