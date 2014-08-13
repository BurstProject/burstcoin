package nxt.http;

import nxt.peer.Hallmark;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_HALLMARK;
import static nxt.http.JSONResponses.MISSING_HALLMARK;

public final class DecodeHallmark extends APIServlet.APIRequestHandler {

    static final DecodeHallmark instance = new DecodeHallmark();

    private DecodeHallmark() {
        super("hallmark");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String hallmarkValue = req.getParameter("hallmark");
        if (hallmarkValue == null) {
            return MISSING_HALLMARK;
        }

        try {

            Hallmark hallmark = Hallmark.parseHallmark(hallmarkValue);

            return JSONData.hallmark(hallmark);

        } catch (RuntimeException e) {
            return INCORRECT_HALLMARK;
        }
    }

}
