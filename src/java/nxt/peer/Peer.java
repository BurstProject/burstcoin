package nxt.peer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public interface Peer extends Comparable<Peer> {

    public static enum State {
        NON_CONNECTED, CONNECTED, DISCONNECTED
    }

    String getPeerAddress();

    String getAnnouncedAddress();

    State getState();

    String getVersion();

    String getApplication();

    String getPlatform();

    String getSoftware();

    Hallmark getHallmark();

    int getWeight();

    boolean shareAddress();

    boolean isWellKnown();

    boolean isBlacklisted();

    void blacklist(Exception cause);

    void blacklist();

    void unBlacklist();

    void deactivate();

    void remove();

    long getDownloadedVolume();

    long getUploadedVolume();

    JSONObject send(JSONStreamAware request);

}
