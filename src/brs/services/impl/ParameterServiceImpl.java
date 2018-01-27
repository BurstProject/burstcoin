package brs.services.impl;

import static brs.http.JSONResponses.HEIGHT_NOT_AVAILABLE;
import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.JSONResponses.INCORRECT_ALIAS;
import static brs.http.JSONResponses.INCORRECT_ASSET;
import static brs.http.JSONResponses.INCORRECT_AT;
import static brs.http.JSONResponses.INCORRECT_ENCRYPTED_MESSAGE;
import static brs.http.JSONResponses.INCORRECT_GOODS;
import static brs.http.JSONResponses.INCORRECT_HEIGHT;
import static brs.http.JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS;
import static brs.http.JSONResponses.INCORRECT_PLAIN_MESSAGE;
import static brs.http.JSONResponses.INCORRECT_PUBLIC_KEY;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;
import static brs.http.JSONResponses.INCORRECT_RECIPIENT;
import static brs.http.JSONResponses.MISSING_ACCOUNT;
import static brs.http.JSONResponses.MISSING_ALIAS_OR_ALIAS_NAME;
import static brs.http.JSONResponses.MISSING_ASSET;
import static brs.http.JSONResponses.MISSING_AT;
import static brs.http.JSONResponses.MISSING_GOODS;
import static brs.http.JSONResponses.MISSING_PURCHASE;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE_OR_PUBLIC_KEY;
import static brs.http.JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
import static brs.http.JSONResponses.UNKNOWN_ACCOUNT;
import static brs.http.JSONResponses.UNKNOWN_ALIAS;
import static brs.http.JSONResponses.UNKNOWN_ASSET;
import static brs.http.JSONResponses.UNKNOWN_AT;
import static brs.http.JSONResponses.UNKNOWN_GOODS;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.AT_PARAMETER;
import static brs.http.common.Parameters.ENCRYPTED_MESSAGE_DATA_PARAMETER;
import static brs.http.common.Parameters.ENCRYPTED_MESSAGE_NONCE_PARAMETER;
import static brs.http.common.Parameters.ENCRYPT_TO_SELF_MESSAGE_DATA;
import static brs.http.common.Parameters.ENCRYPT_TO_SELF_MESSAGE_NONCE;
import static brs.http.common.Parameters.GOODS_PARAMETER;
import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER;
import static brs.http.common.Parameters.NUMBER_OF_CONFIRMATIONS_PARAMETER;
import static brs.http.common.Parameters.PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.PURCHASE_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.AT;
import brs.Account;
import brs.Alias;
import brs.Asset;
import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.BurstException;
import brs.DigitalGoodsStore;
import brs.Transaction;
import brs.TransactionProcessor;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.http.ParameterException;
import brs.http.common.Parameters;
import brs.services.ATService;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetService;
import brs.services.DGSGoodsStoreService;
import brs.services.ParameterService;
import brs.util.Convert;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterServiceImpl implements ParameterService {

  private static final Logger logger = LoggerFactory.getLogger(ParameterServiceImpl.class);

  private final AccountService accountService;
  private final AliasService aliasService;
  private final AssetService assetService;
  private final DGSGoodsStoreService dgsGoodsStoreService;
  private final Blockchain blockchain;
  private final BlockchainProcessor blockchainProcessor;
  private final TransactionProcessor transactionProcessor;
  private final ATService atService;

  public ParameterServiceImpl(AccountService accountService, AliasService aliasService, AssetService assetService, DGSGoodsStoreService dgsGoodsStoreService, Blockchain blockchain,
      BlockchainProcessor blockchainProcessor,
      TransactionProcessor transactionProcessor, ATService atService) {
    this.accountService = accountService;
    this.aliasService = aliasService;
    this.assetService = assetService;
    this.dgsGoodsStoreService = dgsGoodsStoreService;
    this.blockchain = blockchain;
    this.blockchainProcessor = blockchainProcessor;
    this.transactionProcessor = transactionProcessor;
    this.atService = atService;
  }

  @Override
  public Account getAccount(HttpServletRequest req) throws BurstException {
    String accountId = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
    if (accountId == null) {
      throw new ParameterException(MISSING_ACCOUNT);
    }
    try {
      Account account = accountService.getAccount(Convert.parseAccountId(accountId));
      if (account == null) {
        throw new ParameterException(UNKNOWN_ACCOUNT);
      }
      return account;
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_ACCOUNT);
    }
  }

  @Override
  public List<Account> getAccounts(HttpServletRequest req) throws ParameterException {
    String[] accountIDs = req.getParameterValues(ACCOUNT_PARAMETER);
    if (accountIDs == null || accountIDs.length == 0) {
      throw new ParameterException(MISSING_ACCOUNT);
    }
    List<Account> result = new ArrayList<>();
    for (String accountValue : accountIDs) {
      if (accountValue == null || accountValue.isEmpty()) {
        continue;
      }
      try {
        Account account = accountService.getAccount(Convert.parseAccountId(accountValue));
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

  @Override
  public Account getSenderAccount(HttpServletRequest req) throws ParameterException {
    Account account;
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    String publicKeyString = Convert.emptyToNull(req.getParameter(PUBLIC_KEY_PARAMETER));
    if (secretPhrase != null) {
      account = accountService.getAccount(Crypto.getPublicKey(secretPhrase));
    } else if (publicKeyString != null) {
      try {
        account = accountService.getAccount(Convert.parseHexString(publicKeyString));
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

  @Override
  public Alias getAlias(HttpServletRequest req) throws ParameterException {
    long aliasId;
    try {
      aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(ALIAS_PARAMETER)));
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_ALIAS);
    }
    String aliasName = Convert.emptyToNull(req.getParameter(ALIAS_NAME_PARAMETER));
    Alias alias;
    if (aliasId != 0) {
      alias = aliasService.getAlias(aliasId);
    } else if (aliasName != null) {
      alias = aliasService.getAlias(aliasName);
    } else {
      throw new ParameterException(MISSING_ALIAS_OR_ALIAS_NAME);
    }
    if (alias == null) {
      throw new ParameterException(UNKNOWN_ALIAS);
    }
    return alias;
  }

  @Override
  public Asset getAsset(HttpServletRequest req) throws ParameterException {
    String assetValue = Convert.emptyToNull(req.getParameter(ASSET_PARAMETER));
    if (assetValue == null) {
      throw new ParameterException(MISSING_ASSET);
    }
    Asset asset;
    try {
      long assetId = Convert.parseUnsignedLong(assetValue);
      asset = assetService.getAsset(assetId);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_ASSET);
    }
    if (asset == null) {
      throw new ParameterException(UNKNOWN_ASSET);
    }
    return asset;
  }

  @Override
  public DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException {
    String goodsValue = Convert.emptyToNull(req.getParameter(GOODS_PARAMETER));
    if (goodsValue == null) {
      throw new ParameterException(MISSING_GOODS);
    }

    try {
      long goodsId = Convert.parseUnsignedLong(goodsValue);
      DigitalGoodsStore.Goods goods = dgsGoodsStoreService.getGoods(goodsId);
      if (goods == null) {
        throw new ParameterException(UNKNOWN_GOODS);
      }
      return goods;
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_GOODS);
    }
  }

  @Override
  public DigitalGoodsStore.Purchase getPurchase(HttpServletRequest req) throws ParameterException {
    String purchaseIdString = Convert.emptyToNull(req.getParameter(PURCHASE_PARAMETER));
    if (purchaseIdString == null) {
      throw new ParameterException(MISSING_PURCHASE);
    }
    try {
      DigitalGoodsStore.Purchase purchase = dgsGoodsStoreService.getPurchase(Convert.parseUnsignedLong(purchaseIdString));
      if (purchase == null) {
        throw new ParameterException(INCORRECT_PURCHASE);
      }
      return purchase;
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_PURCHASE);
    }
  }

  @Override
  public EncryptedData getEncryptedMessage(HttpServletRequest req, Account recipientAccount) throws ParameterException {
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
    boolean isText = !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER));
    try {
      byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
      return recipientAccount.encryptTo(plainMessageBytes, secretPhrase);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
    }
  }

  @Override
  public EncryptedData getEncryptToSelfMessage(HttpServletRequest req) throws ParameterException {
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
    Account senderAccount = accountService.getAccount(Crypto.getPublicKey(secretPhrase));
    boolean isText = !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER));
    try {
      byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
      return senderAccount.encryptTo(plainMessageBytes, secretPhrase);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
    }
  }

  @Override
  public String getSecretPhrase(HttpServletRequest req) throws ParameterException {
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    if (secretPhrase == null) {
      throw new ParameterException(MISSING_SECRET_PHRASE);
    }
    return secretPhrase;
  }

  @Override
  public int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException {
    String numberOfConfirmationsValue = Convert.emptyToNull(req.getParameter(NUMBER_OF_CONFIRMATIONS_PARAMETER));
    if (numberOfConfirmationsValue != null) {
      try {
        int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
        if (numberOfConfirmations <= blockchain.getHeight()) {
          return numberOfConfirmations;
        }
        throw new ParameterException(INCORRECT_NUMBER_OF_CONFIRMATIONS);
      } catch (NumberFormatException e) {
        throw new ParameterException(INCORRECT_NUMBER_OF_CONFIRMATIONS);
      }
    }
    return 0;
  }

  @Override
  public int getHeight(HttpServletRequest req) throws ParameterException {
    String heightValue = Convert.emptyToNull(req.getParameter(HEIGHT_PARAMETER));
    if (heightValue != null) {
      try {
        int height = Integer.parseInt(heightValue);
        if (height < 0 || height > blockchain.getHeight()) {
          throw new ParameterException(INCORRECT_HEIGHT);
        }
        if (height < blockchainProcessor.getMinRollbackHeight()) {
          throw new ParameterException(HEIGHT_NOT_AVAILABLE);
        }
        return height;
      } catch (NumberFormatException e) {
        throw new ParameterException(INCORRECT_HEIGHT);
      }
    }
    return -1;
  }

  @Override
  public Transaction parseTransaction(String transactionBytes, String transactionJSON) throws ParameterException {
    if (transactionBytes == null && transactionJSON == null) {
      throw new ParameterException(MISSING_TRANSACTION_BYTES_OR_JSON);
    }
    if (transactionBytes != null) {
      try {
        byte[] bytes = Convert.parseHexString(transactionBytes);
        return transactionProcessor.parseTransaction(bytes);
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
        return transactionProcessor.parseTransaction(json);
      } catch (BurstException.ValidationException | RuntimeException | ParseException e) {
        logger.debug(e.getMessage(), e);
        JSONObject response = new JSONObject();
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect transactionJSON: " + e.toString());
        throw new ParameterException(response);
      }
    }
  }

  @Override
  public AT getAT(HttpServletRequest req) throws ParameterException {
    String atValue = Convert.emptyToNull(req.getParameter(AT_PARAMETER));
    if (atValue == null) {
      throw new ParameterException(MISSING_AT);
    }
    AT at;
    try {
      Long atId = Convert.parseUnsignedLong(atValue);
      at = atService.getAT(atId);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_AT);
    }
    if (at == null) {
      throw new ParameterException(UNKNOWN_AT);
    }
    return at;
  }
}
