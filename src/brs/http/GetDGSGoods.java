package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.IN_STOCK_ONLY_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.SELLER_PARAMETER;
import static brs.http.common.ResultFields.GOODS_RESPONSE;

import brs.DigitalGoodsStore;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.db.sql.DbUtils;
import brs.http.common.Parameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGoods extends APIServlet.APIRequestHandler {

  public GetDGSGoods() {
    super(new APITag[] {APITag.DGS}, SELLER_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, IN_STOCK_ONLY_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long sellerId = ParameterParser.getSellerId(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    boolean inStockOnly = !Parameters.isFalse(req.getParameter(IN_STOCK_ONLY_PARAMETER));

    JSONObject response = new JSONObject();
    JSONArray goodsJSON = new JSONArray();
    response.put(GOODS_RESPONSE, goodsJSON);

    BurstIterator<DigitalGoodsStore.Goods> goods = null;
    try {
      if (sellerId == 0) {
        if (inStockOnly) {
          goods = DigitalGoodsStore.getGoodsInStock(firstIndex, lastIndex);
        } else {
          goods = DigitalGoodsStore.getAllGoods(firstIndex, lastIndex);
        }
      } else {
        goods = DigitalGoodsStore.getSellerGoods(sellerId, inStockOnly, firstIndex, lastIndex);
      }
      while (goods.hasNext()) {
        DigitalGoodsStore.Goods good = goods.next();
        goodsJSON.add(JSONData.goods(good));
      }
    } finally {
      DbUtils.close(goods);
    }

    return response;
  }

}
