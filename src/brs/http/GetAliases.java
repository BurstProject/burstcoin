package brs.http;

import brs.Alias;
import brs.BurstException;
import brs.util.FilteringIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliases extends APIServlet.APIRequestHandler {

  static final GetAliases instance = new GetAliases();

  private GetAliases() {
    super(new APITag[] {APITag.ALIASES}, "timestamp", "account", "firstIndex", "lastIndex");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final int timestamp = ParameterParser.getTimestamp(req);
    final long accountId = ParameterParser.getAccount(req).getId();
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray aliases = new JSONArray();
    try (FilteringIterator<Alias> aliasIterator = new FilteringIterator<Alias>(Alias.getAliasesByOwner(accountId, 0, -1),
                                                                               new FilteringIterator.Filter<Alias>() {
                                                                                 @Override
                                                                                 public boolean ok(Alias alias) {
                                                                                   return alias.getTimestamp() >= timestamp;
                                                                                 }
                                                                               }, firstIndex, lastIndex)) {
      while(aliasIterator.hasNext()) {
        aliases.add(JSONData.alias(aliasIterator.next()));
      }
    }

    JSONObject response = new JSONObject();
    response.put("aliases", aliases);
    return response;
  }

}
