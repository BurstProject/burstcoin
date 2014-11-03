package nxt.http;

import nxt.Poll;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPollIds extends APIServlet.APIRequestHandler {

    static final GetPollIds instance = new GetPollIds();

    private GetPollIds() {
        super(new APITag[] {APITag.VS}, "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray pollIds = new JSONArray();
        try (DbIterator<Poll> polls = Poll.getAllPolls(firstIndex, lastIndex)) {
            while (polls.hasNext()) {
                pollIds.add(Convert.toUnsignedLong(polls.next().getId()));
            }
        }
        JSONObject response = new JSONObject();
        response.put("pollIds", pollIds);
        return response;

    }

}
