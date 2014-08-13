package nxt.peer;

import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PeerServlet extends HttpServlet {

    abstract static class PeerRequestHandler {
        abstract JSONStreamAware processRequest(JSONObject request, Peer peer);
    }

    private static final Map<String,PeerRequestHandler> peerRequestHandlers;

    static {
        Map<String,PeerRequestHandler> map = new HashMap<>();
        map.put("getCumulativeDifficulty", GetCumulativeDifficulty.instance);
        map.put("getInfo", GetInfo.instance);
        map.put("getMilestoneBlockIds", GetMilestoneBlockIds.instance);
        map.put("getNextBlockIds", GetNextBlockIds.instance);
        map.put("getNextBlocks", GetNextBlocks.instance);
        map.put("getPeers", GetPeers.instance);
        map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
        map.put("processBlock", ProcessBlock.instance);
        map.put("processTransactions", ProcessTransactions.instance);
        peerRequestHandlers = Collections.unmodifiableMap(map);
    }

    private static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
    static {
        JSONObject response = new JSONObject();
        response.put("error", "Unsupported request type!");
        UNSUPPORTED_REQUEST_TYPE = JSON.prepare(response);
    }

    private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
    static {
        JSONObject response = new JSONObject();
        response.put("error", "Unsupported protocol!");
        UNSUPPORTED_PROTOCOL = JSON.prepare(response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PeerImpl peer = null;
        JSONStreamAware response;

        try {
            String remoteAddr = req.getRemoteAddr();
            if (remoteAddr.indexOf(':') >= 0)
                remoteAddr = "["+remoteAddr+"]";
            peer = Peers.addPeer(remoteAddr, null);
            if (peer == null) {
                //Logger.logDebugMessage("Rejected request from "+remoteAddr);
                return;
            }
            if (peer.isBlacklisted()) {
                //Logger.logDebugMessage("Rejected request from blacklisted peer "+remoteAddr);
                return;
            }

            JSONObject request;
            CountingInputStream cis = new CountingInputStream(req.getInputStream());
            try (Reader reader = new BufferedReader(new InputStreamReader(cis, "UTF-8"))) {
                request = (JSONObject) JSONValue.parse(reader);
            }
            if (request == null) {
                return;
            }

            if (peer.getState() == Peer.State.DISCONNECTED) {
                peer.setState(Peer.State.CONNECTED);
            }
            peer.updateDownloadedVolume(cis.getCount());
            if (! peer.analyzeHallmark(peer.getPeerAddress(), (String)request.get("hallmark"))) {
                peer.blacklist();
                return;
            }

            if (request.get("protocol") != null && ((String)request.get("protocol")).equals("B1")) {
                PeerRequestHandler peerRequestHandler = peerRequestHandlers.get(request.get("requestType"));
                if (peerRequestHandler != null) {
                    response = peerRequestHandler.processRequest(request, peer);
                } else {
                    response = UNSUPPORTED_REQUEST_TYPE;
                }
            } else {
                Logger.logDebugMessage("Unsupported protocol " + request.get("protocol"));
                response = UNSUPPORTED_PROTOCOL;
            }

        } catch (RuntimeException e) {
            Logger.logDebugMessage("Error processing POST request", e);
            JSONObject json = new JSONObject();
            json.put("error", e.toString());
            response = json;
        }

        resp.setContentType("text/plain; charset=UTF-8");
        CountingOutputStream cos = new CountingOutputStream(resp.getOutputStream());
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(cos, "UTF-8"))) {
            response.writeJSONString(writer);
        }

        if (peer != null) {
            peer.updateUploadedVolume(cos.getCount());
        }
    }

}
