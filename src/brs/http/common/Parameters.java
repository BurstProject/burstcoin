package brs.http.common;

public class Parameters {

  public static final String ALIAS_PARAMETER = "alias";
  public static final String AMOUNT_NQT_PARAMETER = "amountNQT";
  public static final String ALIAS_NAME_PARAMETER = "aliasName";
  public static final String FEE_QT_PARAMETER = "feeNQT";
  public static final String PRICE_NQT_PARAMETER = "priceNQT";
  public static final String QUANTITY_NQT_PARAMETER = "quantityQNT";
  public static final String ASSET_PARAMETER = "asset";
  public static final String GOODS_PARAMETER = "goods";
  public static final String ORDER_PARAMETER = "order";
  public static final String QUANTITY_PARAMETER = "quantity";
  public static final String ENCRYPTED_MESSAGE_DATA_PARAMETER = "encryptedMessageData";
  public static final String ENCRYPTED_MESSAGE_NONCE_PARAMETER = "encryptedMessageNonce";
  public static final String MESSAGE_TO_ENCRYPT_PARAMETER = "messageToEncrypt";
  public static final String MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER = "messageToEncryptIsText";
  public static final String ENCRYPT_TO_SELF_MESSAGE_DATA = "encryptToSelfMessageData";
  public static final String ENCRYPT_TO_SELF_MESSAGE_NONCE = "encryptToSelfMessageNonce";
  public static final String MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER = "messageToEncryptToSelf";
  public static final String MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER = "messageToEncryptToSelfIsText";
  public static final String GOODS_DATA_PARAMETER = "goodsData";
  public static final String GOODS_NONCE_PARAMETER = "goodsNonce";
  public static final String PURCHASE_PARAMETER = "purchase";
  public static final String SECRET_PHRASE_PARAMETER = "secretPhrase";
  public static final String PUBLIC_KEY_PARAMETER = "publicKey";
  public static final String ACCOUNT_PARAMETER = "account";
  public static final String TIMESTAMP_PARAMETER = "timestamp";
  public static final String RECIPIENT_PARAMETER = "recipient";
  public static final String SELLER_PARAMETER = "seller";
  public static final String BUYER_PARAMETER = "buyer";
  public static final String FIRST_INDEX_PARAMETER = "firstIndex";
  public static final String LAST_INDEX_PARAMETER = "lastIndex";
  public static final String NUMBER_OF_CONFIRMATIONS_PARAMETER = "numberOfConfirmations";
  public static final String HEIGHT_PARAMETER = "height";

  public static boolean isFalse(String text) {
    return "false".equalsIgnoreCase(text);
  }

  public static boolean isZero(String recipientValue) {
    return "0".equals(recipientValue);
  }
}
