package brs.http;

import static brs.http.ParameterParser.ALIAS_PARAMETER;

import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

public class ParameterParserTest {

  @Test
  public void getAlias() {
    //TODO Happy path
  }

  @Test(expected = ParameterException.class)
  public void getAlias_incorrectAliasBecauseUnsignedLong() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ALIAS_PARAMETER, "Blaat"));
    ParameterParser.getAlias(req);
  }

  @Test
  public void getAmountNQT() {
    //TODO Happy path
  }

  @Test
  public void getFeeNQT() {
    //TODO Happy path
  }

  @Test
  public void getPriceNQT() {
    //TODO Happy path
  }

  @Test
  public void getAsset() {
    //TODO Happy path
  }

  @Test
  public void getQuantityQNT() {
    //TODO Happy path
  }

  @Test
  public void getOrderId() {
    //TODO Happy path
  }

  @Test
  public void getGoods() {
    //TODO Happy path
  }

  @Test
  public void getGoodsQuantity() {
    //TODO Happy path
  }

  @Test
  public void getEncryptedMessage() {
    //TODO Happy path
  }

  @Test
  public void getEncryptToSelfMessage() {
    //TODO Happy path
  }

  @Test
  public void getEncryptedGoods() {
    //TODO Happy path
  }

  @Test
  public void getPurchase() {
    //TODO Happy path
  }

  @Test
  public void getSecretPhrase() {
    //TODO Happy path
  }

  @Test
  public void getSenderAccount() {
    //TODO Happy path
  }

  @Test
  public void getAccount() {
    //TODO Happy path
  }

  @Test
  public void getAccounts() {
    //TODO Happy path
  }

  @Test
  public void getTimestamp() {
    //TODO Happy path
  }

  @Test
  public void getRecipientId() {
    //TODO Happy path
  }

  @Test
  public void getSellerId() {
    //TODO Happy path
  }

  @Test
  public void getBuyerId() {
    //TODO Happy path
  }

  @Test
  public void getFirstIndex() {
    //TODO Happy path
  }

  @Test
  public void getLastIndex() {
    //TODO Happy path
  }

  @Test
  public void getNumberOfConfirmations() {
    //TODO Happy path
  }

  @Test
  public void getHeight() {
    //TODO Happy path
  }

  @Test
  public void parseTransaction() {
    //TODO Happy path
  }

  @Test
  public void getAT() {
    //TODO Happy path
  }

  @Test
  public void getCreationBytes() {
    //TODO Happy path
  }

  @Test
  public void getATLong() {
    //TODO Happy path
  }
}