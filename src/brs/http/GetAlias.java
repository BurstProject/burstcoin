package brs.http;

import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;

import brs.Alias;
import brs.Alias.Offer;
import brs.services.AliasService;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAlias extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AliasService aliasService;

  GetAlias(ParameterService parameterService, AliasService aliasService) {
    super(new APITag[] {APITag.ALIASES}, ALIAS_PARAMETER, ALIAS_NAME_PARAMETER);
    this.parameterService = parameterService;
    this.aliasService = aliasService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
    final Alias alias = parameterService.getAlias(req);
    final Offer offer = aliasService.getOffer(alias);

    return JSONData.alias(alias, offer);
  }

}
