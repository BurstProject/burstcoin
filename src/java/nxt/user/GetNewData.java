package nxt.user;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public final class GetNewData extends UserServlet.UserRequestHandler {

    static final GetNewData instance = new GetNewData();

    private GetNewData() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        return null;
    }
}
