package brs.services;

import brs.AT;
import brs.Account;
import brs.Alias;
import brs.Asset;
import brs.BurstException;
import brs.DigitalGoodsStore;
import brs.Transaction;
import brs.crypto.EncryptedData;
import brs.http.ParameterException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public interface ParameterService {

  Account getAccount(HttpServletRequest req) throws BurstException;

  List<Account> getAccounts(HttpServletRequest req) throws ParameterException;

  Account getSenderAccount(HttpServletRequest req) throws ParameterException;

  Alias getAlias(HttpServletRequest req) throws ParameterException;

  Asset getAsset(HttpServletRequest req) throws ParameterException;

  DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException;

  DigitalGoodsStore.Purchase getPurchase(HttpServletRequest req) throws ParameterException;

  EncryptedData getEncryptedMessage(HttpServletRequest req, Account recipientAccount) throws ParameterException;

  EncryptedData getEncryptToSelfMessage(HttpServletRequest req) throws ParameterException;

  String getSecretPhrase(HttpServletRequest req) throws ParameterException;

  int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException;

  int getHeight(HttpServletRequest req) throws ParameterException;

  Transaction parseTransaction(String transactionBytes, String transactionJSON) throws ParameterException;

  AT getAT(HttpServletRequest req) throws ParameterException;
}
