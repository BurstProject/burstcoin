package brs.peer;

import brs.services.TimeService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInfo extends PeerServlet.PeerRequestHandler {

  private final TimeService timeService;

  GetInfo(TimeService timeService) {
    this.timeService = timeService;
  }

  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {
    PeerImpl peerImpl = (PeerImpl)peer;
    String announcedAddress = (String)request.get("announcedAddress");
    if (announcedAddress != null && ! (announcedAddress = announcedAddress.trim()).isEmpty()) {
      if (peerImpl.getAnnouncedAddress() != null && ! announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
        // force verification of changed announced address
        peerImpl.setState(Peer.State.NON_CONNECTED);
      }
      peerImpl.setAnnouncedAddress(announcedAddress);
    }
    String application = (String)request.get("application");
    if (application == null) {
      application = "?";
    }
    peerImpl.setApplication(application.trim());

    String version = (String)request.get("version");
    if (version == null) {
      version = "?";
    }
    peerImpl.setVersion(version.trim());

    String platform = (String)request.get("platform");
    if (platform == null) {
      platform = "?";
    }
    peerImpl.setPlatform(platform.trim());

    peerImpl.setShareAddress(Boolean.TRUE.equals(request.get("shareAddress")));
    peerImpl.setLastUpdated(timeService.getEpochTime());

    //peerImpl.setState(Peer.State.CONNECTED);
    Peers.notifyListeners(peerImpl, Peers.Event.ADDED_ACTIVE_PEER);

    return Peers.myPeerInfoResponse;

  }

}
