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

  boolean isAtLeastMyVersion();

  boolean isHigherOrEqualVersionThan(String version);

  void blacklist(Exception cause, String description);

  void blacklist(String description);

  void blacklist();

  void unBlacklist();

  void remove();

  long getDownloadedVolume();

  long getUploadedVolume();

  int getLastUpdated();

  Long getLastUnconfirmedTransactionTimestamp();

  void setLastUnconfirmedTransactionTimestamp(Long lastUnconfirmedTransactionTimestamp);

  JSONObject send(JSONStreamAware request);

}
