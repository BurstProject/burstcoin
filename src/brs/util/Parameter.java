package brs.util;

public class Parameter {

  public static boolean isFalse(String text) {
    return "false".equalsIgnoreCase(text);
  }

  public static boolean isZero(String recipientValue) {
    return "0".equals(recipientValue);
  }
}
