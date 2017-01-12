package Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import Algorithm.RoutePlanningService;
import Util.LatLon;
import Util.SimplifiedTMSOrder;
import Util.TMSOrderLabel;

/**
 * Created by Kelvin Liu on Aug 2nd, 2016
 */
public class RouteAssemblyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {String requestParams = httpExchange.getRequestURI().getQuery();
        Map<String, String> requestMapping = BalancingHandler.queryToMap(requestParams);
        String depot = requestMapping.get("customerDepot");
        SimplifiedTMSOrder dtp = null;
        String points = requestMapping.get("points");
        JSONArray jsa = new JSONArray(points);
        List<SimplifiedTMSOrder> ordersToBalance = new ArrayList<>();
        double speed = 40.;
        Date departureTime = new Date();
        boolean isVolumnLimit = false;
        double volumnLimit = 50.;

		// speed = Double.parseDouble(requestMapping.get("speed"));
		// departureTime =
		// BalancingHandler.queryToDate(requestMapping.get("departureTime"));
		// isVolumnLimit = Boolean.valueOf(requestMapping.get("isVolumnLimit"));
		// if(isVolumnLimit == true) {
		// volumnLimit = Double.parseDouble(requestMapping.get("volumnLimit"));
		// }

        if (depot != null) {
            String[] keys = depot.split(";");
            LatLon depotCustLatLng = new LatLon(keys[0], keys[1]);
            dtp = new SimplifiedTMSOrder(new TMSOrderLabel("00000000", "heheda"), depotCustLatLng);
        }

        for (int i = 0; i < jsa.length(); i++) {
            JSONObject currentObj = jsa.getJSONObject(i);
            JSONObject latlonObj = currentObj.getJSONObject("latlon");
            JSONObject orderLabelObj = currentObj.getJSONObject("orderLabel");
            SimplifiedTMSOrder candidate = new SimplifiedTMSOrder(new TMSOrderLabel(orderLabelObj.getString("orderNumber"), orderLabelObj.getString("orderAddr")),
                    new LatLon(latlonObj.getDouble("lat"), latlonObj.getDouble("lon")));
            ordersToBalance.add(candidate);
        }

        RoutePlanningService rps = new RoutePlanningService(ordersToBalance, speed, departureTime, isVolumnLimit, volumnLimit);
        List<List<SimplifiedTMSOrder>> balancedOrders = rps.getOptimalPlan(ordersToBalance.size(), ordersToBalance.size(), true, dtp, false);
        List<Integer> distancesReturned = RoutePlanningService.getDistances(balancedOrders);
        System.out.println("这条路线的距离是:" + distancesReturned.get(0));
        JSONObject returningObj = new JSONObject();
        returningObj.put("orderedPoints", balancedOrders);
        returningObj.put("distance", distancesReturned.get(0));

        String returnedStr = returningObj.toString(4);
        httpExchange.sendResponseHeaders(200, returnedStr.getBytes().length);
        httpExchange.getResponseBody().write(returnedStr.getBytes());
        httpExchange.getResponseBody().close();
    }
}
