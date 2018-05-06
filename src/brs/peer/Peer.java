package brs.peer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public interface Peer extends Comparable<Peer> {

  enum State {
    NON_CONNECTED, CONNECTED, DISCONNECTED
  }

  String getPeerAddress();

  String getAnnouncedAddress();

  State getState();

  String getVersion();

  String getApplication();

  String getPlatform();

  String getSoftware();

  boolean shareAddress();

  boolean isWellKnown();

  boolean isRebroadcastTarget();

  boolean isBlacklisted();

  void blacklist(Exception cause);

  void blacklist();

  void unBlacklist();

  void deactivate();

  void remove();

  long getDownloadedVolume();

  long getUploadedVolume();

  int getLastUpdated();

  JSONObject send(JSONStreamAware request);

  /** 
   * Sends a simple GET-Request to the peer and returns the JSON-Object of the response
   * @param  pathAndQuery  URL path for GET request
   * @return JSONObject of the response
   */
  JSONObject sendGetRequest(String pathAndQuery);


}
