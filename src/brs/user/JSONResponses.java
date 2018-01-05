package brs.user;

import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.Constants.*;

public final class JSONResponses {

  public static final JSONStreamAware INVALID_SECRET_PHRASE;
  static {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, "showMessage");
    response.put("message", "Invalid secret phrase!");
    INVALID_SECRET_PHRASE = JSON.prepare(response);
  }

  public static final JSONStreamAware LOCK_ACCOUNT;
  static {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, "lockAccount");
    LOCK_ACCOUNT = JSON.prepare(response);
  }

  public static final JSONStreamAware LOCAL_USERS_ONLY;
  static {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, "showMessage");
    response.put("message", "This operation is allowed to local host users only!");
    LOCAL_USERS_ONLY = JSON.prepare(response);
  }

  public static final JSONStreamAware NOTIFY_OF_ACCEPTED_TRANSACTION;
  static {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, "notifyOfAcceptedTransaction");
    NOTIFY_OF_ACCEPTED_TRANSACTION = JSON.prepare(response);
  }

  public static final JSONStreamAware DENY_ACCESS;
  static {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, "denyAccess");
    DENY_ACCESS = JSON.prepare(response);
  }

  public static final JSONStreamAware INCORRECT_REQUEST;
  static {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, "showMessage");
    response.put("message", "Incorrect request!");
    INCORRECT_REQUEST = JSON.prepare(response);
  }

  public static final JSONStreamAware POST_REQUIRED;
  static {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, "showMessage");
    response.put("message", "This request is only accepted using POST!");
    POST_REQUIRED = JSON.prepare(response);
  }

  private JSONResponses() {} // never

}
