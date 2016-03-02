package nxt.user;

import nxt.peer.Peer;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

import static nxt.user.JSONResponses.LOCAL_USERS_ONLY;

public final class RemoveActivePeer extends UserServlet.UserRequestHandler {

    static final RemoveActivePeer instance = new RemoveActivePeer();

    private RemoveActivePeer() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        if (Users.allowedUserHosts == null && ! InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
            return LOCAL_USERS_ONLY;
        } else {
            int index = Integer.parseInt(req.getParameter("peer"));
            Peer peer= Users.getPeer(index);
            if (peer != null && ! peer.isBlacklisted()) {
                peer.deactivate();
            }
        }
        return null;
    }
}
