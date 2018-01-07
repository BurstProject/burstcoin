package brs.http;

import static brs.http.JSONResponses.HEIGHT_NOT_AVAILABLE;
import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.JSONResponses.INCORRECT_ALIAS;
import static brs.http.JSONResponses.INCORRECT_AMOUNT;
import static brs.http.JSONResponses.INCORRECT_ASSET;
import static brs.http.JSONResponses.INCORRECT_ASSET_QUANTITY;
import static brs.http.JSONResponses.INCORRECT_AT;
import static brs.http.JSONResponses.INCORRECT_CREATION_BYTES;
import static brs.http.JSONResponses.INCORRECT_DGS_ENCRYPTED_GOODS;
import static brs.http.JSONResponses.INCORRECT_ENCRYPTED_MESSAGE;
import static brs.http.JSONResponses.INCORRECT_FEE;
import static brs.http.JSONResponses.INCORRECT_GOODS;
import static brs.http.JSONResponses.INCORRECT_HEIGHT;
import static brs.http.JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS;
import static brs.http.JSONResponses.INCORRECT_ORDER;
import static brs.http.JSONResponses.INCORRECT_PLAIN_MESSAGE;
import static brs.http.JSONResponses.INCORRECT_PRICE;
import static brs.http.JSONResponses.INCORRECT_PUBLIC_KEY;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;
import static brs.http.JSONResponses.INCORRECT_QUANTITY;
import static brs.http.JSONResponses.INCORRECT_RECIPIENT;
import static brs.http.JSONResponses.INCORRECT_TIMESTAMP;
import static brs.http.JSONResponses.MISSING_ACCOUNT;
import static brs.http.JSONResponses.MISSING_ALIAS_OR_ALIAS_NAME;
import static brs.http.JSONResponses.MISSING_AMOUNT;
import static brs.http.JSONResponses.MISSING_ASSET;
import static brs.http.JSONResponses.MISSING_AT;
import static brs.http.JSONResponses.MISSING_FEE;
import static brs.http.JSONResponses.MISSING_GOODS;
import static brs.http.JSONResponses.MISSING_ORDER;
import static brs.http.JSONResponses.MISSING_PRICE;
import static brs.http.JSONResponses.MISSING_PURCHASE;
import static brs.http.JSONResponses.MISSING_QUANTITY;
import static brs.http.JSONResponses.MISSING_RECIPIENT;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE_OR_PUBLIC_KEY;
import static brs.http.JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
import static brs.http.JSONResponses.UNKNOWN_ACCOUNT;
import static brs.http.JSONResponses.UNKNOWN_ALIAS;
import static brs.http.JSONResponses.UNKNOWN_ASSET;
import static brs.http.JSONResponses.UNKNOWN_AT;
import static brs.http.JSONResponses.UNKNOWN_GOODS;

import brs.AT;
import brs.Account;
import brs.Alias;
import brs.Asset;
import brs.Burst;
import brs.BurstException;
import brs.Constants;
import brs.DigitalGoodsStore;
import brs.Transaction;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.util.Convert;
import brs.util.Parameter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ParameterParser {

  private static final Logger logger = LoggerFactory.getLogger(ParameterParser.class);

  static final String ALIAS_PARAMETER = "alias";
  static final String AMOUNT_NQT_PARAMETER = "amountNQT";
  static final String ALIAS_NAME_PARAMETER = "aliasName";
  static final String FEE_QT_PARAMETER = "feeNQT";
  static final String PRICE_NQT_PARAMETER = "priceNQT";
  static final String QUANTITY_NQT_PARAMETER = "quantityQNT";
  static final String ASSET_PARAMETER = "asset";
  static final String GOODS_PARAMETER = "goods";
  static final String ORDER_PARAMETER = "order";
  static final String QUANTITY_PARAMETER = "quantity";
  static final String ENCRYPTED_MESSAGE_DATA_PARAMETER = "encryptedMessageData";
  static final String ENCRYPTED_MESSAGE_NONCE_PARAMETER = "encryptedMessageNonce";
  static final String MESSAGE_TO_ENCRYPT_PARAMETER = "messageToEncrypt";
  static final String MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER = "messageToEncryptIsText";
  static final String ENCRYPT_TO_SELF_MESSAGE_DATA = "encryptToSelfMessageData";
  static final String ENCRYPT_TO_SELF_MESSAGE_NONCE = "encryptToSelfMessageNonce";
  static final String MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER = "messageToEncryptToSelf";
  static final String MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER = "messageToEncryptToSelfIsText";
  static final String GOODS_DATA_PARAMETER = "goodsData";
  static final String GOODS_NONCE_PARAMETER = "goodsNonce";
  static final String PURCHASE_PARAMETER = "purchase";
  static final String SECRET_PHRASE_PARAMETER = "secretPhrase";
  static final String PUBLIC_KEY_PARAMETER = "publicKey";
  static final String ACCOUNT_PARAMETER = "account";
  static final String TIMESTAMP_PARAMETER = "timestamp";
  static final String RECIPIENT_PARAMETER = "recipient";
  static final String SELLER_PARAMETER = "seller";
  static final String BUYER_PARAMETER = "buyer";
  static final String FIRST_INDEX_PARAMETER = "firstIndex";
  static final String LAST_INDEX_PARAMETER = "lastIndex";
  static final String NUMBER_OF_CONFIRMATIONS_PARAMETER = "numberOfConfirmations";
  static final String HEIGHT_PARAMETER = "height";

  static final String ERROR_CODE_RESPONSE = "errorCode";
  static final String ERROR_DESCRIPTION_RESPONSE = "errorDescription";
  static final String AT_PARAMETER = "at";
  static final String CREATION_BYTES_PARAMETER = "creationBytes";
  static final String HEX_STRING_PARAMETER = "hexString";

  static Alias getAlias(HttpServletRequest req) throws ParameterException {
    long aliasId;
    try {
      aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(ALIAS_PARAMETER)));
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_ALIAS);
    }
    String aliasName = Convert.emptyToNull(req.getParameter(ALIAS_NAME_PARAMETER));
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
    String amountValueNQT = Convert.emptyToNull(req.getParameter(AMOUNT_NQT_PARAMETER));
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
    String feeValueNQT = Convert.emptyToNull(req.getParameter(FEE_QT_PARAMETER));
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
    String priceValueNQT = Convert.emptyToNull(req.getParameter(PRICE_NQT_PARAMETER));
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
    String assetValue = Convert.emptyToNull(req.getParameter(ASSET_PARAMETER));
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
    String quantityValueQNT = Convert.emptyToNull(req.getParameter(QUANTITY_NQT_PARAMETER));
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
    String orderValue = Convert.emptyToNull(req.getParameter(ORDER_PARAMETER));
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
    String goodsValue = Convert.emptyToNull(req.getParameter(GOODS_PARAMETER));
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
    String quantityString = Convert.emptyToNull(req.getParameter(QUANTITY_PARAMETER));
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
    String data = Convert.emptyToNull(req.getParameter(ENCRYPTED_MESSAGE_DATA_PARAMETER));
    String nonce = Convert.emptyToNull(req.getParameter(ENCRYPTED_MESSAGE_NONCE_PARAMETER));
    if (data != null && nonce != null) {
      try {
        return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
      }
    }
    String plainMessage = Convert.emptyToNull(req.getParameter(MESSAGE_TO_ENCRYPT_PARAMETER));
    if (plainMessage == null) {
      return null;
    }
    if (recipientAccount == null) {
      throw new ParameterException(INCORRECT_RECIPIENT);
    }
    String secretPhrase = getSecretPhrase(req);
    boolean isText = !Parameter.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER));
    try {
      byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
      return recipientAccount.encryptTo(plainMessageBytes, secretPhrase);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
    }
  }

  static EncryptedData getEncryptToSelfMessage(HttpServletRequest req) throws ParameterException {
    String data = Convert.emptyToNull(req.getParameter(ENCRYPT_TO_SELF_MESSAGE_DATA));
    String nonce = Convert.emptyToNull(req.getParameter(ENCRYPT_TO_SELF_MESSAGE_NONCE));
    if (data != null && nonce != null) {
      try {
        return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
      }
    }
    String plainMessage = Convert.emptyToNull(req.getParameter(MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER));
    if (plainMessage == null) {
      return null;
    }
    String secretPhrase = getSecretPhrase(req);
    Account senderAccount = Account.getAccount(Crypto.getPublicKey(secretPhrase));
    boolean isText = !Parameter.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER));
    try {
      byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
      return senderAccount.encryptTo(plainMessageBytes, secretPhrase);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
    }
  }

  static EncryptedData getEncryptedGoods(HttpServletRequest req) throws ParameterException {
    String data = Convert.emptyToNull(req.getParameter(GOODS_DATA_PARAMETER));
    String nonce = Convert.emptyToNull(req.getParameter(GOODS_NONCE_PARAMETER));
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
    String purchaseIdString = Convert.emptyToNull(req.getParameter(PURCHASE_PARAMETER));
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
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    if (secretPhrase == null) {
      throw new ParameterException(MISSING_SECRET_PHRASE);
    }
    return secretPhrase;
  }

  static Account getSenderAccount(HttpServletRequest req) throws ParameterException {
    Account account;
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    String publicKeyString = Convert.emptyToNull(req.getParameter(PUBLIC_KEY_PARAMETER));
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
    String accountValue = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
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
    String[] accountValues = req.getParameterValues(ACCOUNT_PARAMETER);
    if (accountValues == null || accountValues.length == 0) {
      throw new ParameterException(MISSING_ACCOUNT);
    }
    List<Account> result = new ArrayList<>();
    for (String accountValue : accountValues) {
      if (accountValue == null || accountValue.isEmpty()) {
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
    String timestampValue = Convert.emptyToNull(req.getParameter(TIMESTAMP_PARAMETER));
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
    String recipientValue = Convert.emptyToNull(req.getParameter(RECIPIENT_PARAMETER));
    if (recipientValue == null || Parameter.isZero(recipientValue)) {
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
    String sellerIdValue = Convert.emptyToNull(req.getParameter(SELLER_PARAMETER));
    try {
      return Convert.parseAccountId(sellerIdValue);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_RECIPIENT);
    }
  }

  static long getBuyerId(HttpServletRequest req) throws ParameterException {
    String buyerIdValue = Convert.emptyToNull(req.getParameter(BUYER_PARAMETER));
    try {
      return Convert.parseAccountId(buyerIdValue);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_RECIPIENT);
    }
  }

  static int getFirstIndex(HttpServletRequest req) {
    int firstIndex;
    try {
      firstIndex = Integer.parseInt(req.getParameter(FIRST_INDEX_PARAMETER));
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
      lastIndex = Integer.parseInt(req.getParameter(LAST_INDEX_PARAMETER));
      if (lastIndex < 0) {
        return Integer.MAX_VALUE;
      }
    } catch (NumberFormatException e) {
      return Integer.MAX_VALUE;
    }
    return lastIndex;
  }

  static int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException {
    String numberOfConfirmationsValue = Convert.emptyToNull(req.getParameter(NUMBER_OF_CONFIRMATIONS_PARAMETER));
    if (numberOfConfirmationsValue != null) {
      try {
        int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
        if (numberOfConfirmations <= Burst.getBlockchain().getHeight()) {
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
    String heightValue = Convert.emptyToNull(req.getParameter(HEIGHT_PARAMETER));
    if (heightValue != null) {
      try {
        int height = Integer.parseInt(heightValue);
        if (height < 0 || height > Burst.getBlockchain().getHeight()) {
          throw new ParameterException(INCORRECT_HEIGHT);
        }
        if (height < Burst.getBlockchainProcessor().getMinRollbackHeight()) {
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
        return Burst.getTransactionProcessor().parseTransaction(bytes);
      } catch (BurstException.ValidationException | RuntimeException e) {
        logger.debug(e.getMessage(), e);
        JSONObject response = new JSONObject();
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect transactionBytes: " + e.toString());
        throw new ParameterException(response);
      }
    } else {
      try {
        JSONObject json = (JSONObject) JSONValue.parseWithException(transactionJSON);
        return Burst.getTransactionProcessor().parseTransaction(json);
      } catch (BurstException.ValidationException | RuntimeException | ParseException e) {
        logger.debug(e.getMessage(), e);
        JSONObject response = new JSONObject();
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect transactionJSON: " + e.toString());
        throw new ParameterException(response);
      }
    }
  }


  private ParameterParser() {
  } // never


  static AT getAT(HttpServletRequest req) throws ParameterException {
    String atValue = Convert.emptyToNull(req.getParameter(AT_PARAMETER));
    if (atValue == null) {
      throw new ParameterException(MISSING_AT);
    }
    AT at;
    try {
      Long atId = Convert.parseUnsignedLong(atValue);
      at = AT.getAT(atId);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_AT);
    }
    if (at == null) {
      throw new ParameterException(UNKNOWN_AT);
    }
    return at;
  }

  public static byte[] getCreationBytes(HttpServletRequest req) throws ParameterException {
    try {
      return Convert.parseHexString(req.getParameter(CREATION_BYTES_PARAMETER));
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_CREATION_BYTES);
    }


  }

  public static String getATLong(HttpServletRequest req) {
    String hex = req.getParameter(HEX_STRING_PARAMETER);
    ByteBuffer bf = ByteBuffer.allocate(8);
    bf.order(ByteOrder.LITTLE_ENDIAN);
    bf.put(Convert.parseHexString(hex));

    String ret = Convert.toUnsignedLong(bf.getLong(0));
    return ret;
  }

}
