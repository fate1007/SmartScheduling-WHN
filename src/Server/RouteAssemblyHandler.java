package Server;

import Algorithm.RoutePlanningService;
import Util.LatLon;
import Util.SimplifiedTMSOrder;
import Util.TMSOrderLabel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Kelvin Liu on Aug 2nd, 2016
 */
public class RouteAssemblyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String requestParams = httpExchange.getRequestURI().getQuery();
        Map<String, String> requestMapping = BalancingHandler.queryToMap(requestParams);
        String points = requestMapping.get("points");
        JSONArray jsa = new JSONArray(points);
        List<SimplifiedTMSOrder> ordersToBalance = new ArrayList<>();
        for (int i = 0; i < jsa.length(); i++) {
            JSONObject currentObj = jsa.getJSONObject(i);
            JSONObject orderLabelObj = currentObj.getJSONObject("orderLabel");
            JSONObject latlonObj = currentObj.getJSONObject("latlon");
            ordersToBalance.add(new SimplifiedTMSOrder(new TMSOrderLabel(orderLabelObj.getString("orderNumber"), orderLabelObj.getString("orderAddr")),
                    new LatLon(latlonObj.getString("lat"), latlonObj.getString("lon"))));
        }

        RoutePlanningService rps = new RoutePlanningService(ordersToBalance);
        List<List<SimplifiedTMSOrder>> balancedOrders = rps.getOptimalPlan(ordersToBalance.size(), ordersToBalance.size());
        JSONObject returningObj = new JSONObject();
        returningObj.put("orderedPoints", balancedOrders);

        String returnedStr = returningObj.toString(4);
        httpExchange.sendResponseHeaders(200, returnedStr.getBytes().length);
        httpExchange.getResponseBody().write(returnedStr.getBytes());
        httpExchange.getResponseBody().close();
    }
}
