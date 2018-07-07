package brs.http;

public abstract class AbstractGetUnconfirmedTransactions extends APIServlet.APIRequestHandler {

  AbstractGetUnconfirmedTransactions(APITag[] apiTags, String... parameters) {
    super(apiTags, parameters);
  }
}
