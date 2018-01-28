package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;

import brs.Alias;
import brs.Alias.Offer;
import brs.BurstException;
import brs.services.AliasService;
import brs.services.ParameterService;
import brs.util.FilteringIterator;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAliases extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AliasService aliasService;

  GetAliases(ParameterService parameterService, AliasService aliasService) {
    super(new APITag[]{APITag.ALIASES}, TIMESTAMP_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
    this.aliasService = aliasService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final int timestamp = ParameterParser.getTimestamp(req);
    final long accountId = parameterService.getAccount(req).getId();
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray aliases = new JSONArray();
    try (FilteringIterator<Alias> aliasIterator = new FilteringIterator<>(Alias.getAliasesByOwner(accountId, 0, -1),
            alias -> alias.getTimestamp() >= timestamp, firstIndex, lastIndex)) {
      while (aliasIterator.hasNext()) {
        final Alias alias = aliasIterator.next();
        final Offer offer = aliasService.getOffer(alias);

        aliases.add(JSONData.alias(alias, offer));
      }
    }

    JSONObject response = new JSONObject();
    response.put("aliases", aliases);
    return response;
  }

}
