package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public final class GetDGSGoods extends APIServlet.APIRequestHandler {

    static final GetDGSGoods instance = new GetDGSGoods();

    private GetDGSGoods() {
        super(new APITag[] {APITag.DGS}, "seller", "firstIndex", "lastIndex", "inStockOnly");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Long sellerId = ParameterParser.getSellerId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

        JSONObject response = new JSONObject();
        JSONArray goodsJSON = new JSONArray();
        response.put("goods", goodsJSON);

        if (sellerId == null) {
            DigitalGoodsStore.Goods[] goods = DigitalGoodsStore.getAllGoods().toArray(new DigitalGoodsStore.Goods[0]);
            for (int i = 0, count = 0; count - 1 <= lastIndex && i < goods.length; i++) {
                DigitalGoodsStore.Goods good = goods[goods.length - 1 - i];
                if (inStockOnly && good.getQuantity() == 0) {
                    continue;
                }
                if (good.isDelisted()) {
                    continue;
                }
                if (count < firstIndex) {
                    count++;
                    continue;
                }
                goodsJSON.add(JSONData.goods(goods[goods.length - 1 - i]));
                count++;
            }
            return response;
        }

        Collection<DigitalGoodsStore.Goods> goods = DigitalGoodsStore.getSellerGoods(sellerId);
        int count = 0;
        for (DigitalGoodsStore.Goods good : goods) {
            if (count > lastIndex) {
                break;
            }
            if (count >= firstIndex) {
                if (inStockOnly && good.getQuantity() == 0) {
                    continue;
                }
                if (good.isDelisted()) {
                    continue;
                }
                goodsJSON.add(JSONData.goods(good));
            }
            count++;
        }
        return response;
    }

}
