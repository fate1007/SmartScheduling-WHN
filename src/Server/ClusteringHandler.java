package Server;

import Algorithm.RoutePlanningService;
import Util.LatLon;
import Util.SimplifiedTMSOrder;
import Util.TMSOrderLabel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static Server.BalancingHandler.queryToMap;

/**
 * Created by Minghao Liu on 8/25/2016.
 */
public class ClusteringHandler implements HttpHandler{

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String requestParams = httpExchange.getRequestURI().getQuery();
        Map<String, String> requestMapping = queryToMap(requestParams);

        int minTour = Integer.parseInt(requestMapping.get("minTour"));
        int maxTour = Integer.parseInt(requestMapping.get("maxTour"));
        String dropoffPointsJson = requestMapping.get("points");
        JSONObject allObj = new JSONObject(dropoffPointsJson);
        JSONArray ja = allObj.getJSONArray("all");
        List<SimplifiedTMSOrder> allOrders = new ArrayList<>();
        for (int i = 0; i < ja.length(); i++) {
            JSONObject singleOrder = ja.getJSONObject(i);
            try {
                SimplifiedTMSOrder currentOrder = new SimplifiedTMSOrder(new TMSOrderLabel(
                        singleOrder.getString("orderNumber"), singleOrder.getString("addr")),
                        new LatLon(singleOrder.getDouble("latitude"), singleOrder.getDouble("longitude")));

                allOrders.add(currentOrder);
            } catch (JSONException ee) {
                ee.printStackTrace();
                continue;
            }
        }

        RoutePlanningService rp = new RoutePlanningService(allOrders);
        List<List<SimplifiedTMSOrder>> optimal = rp.getOptimalPlan(minTour, maxTour, false, null, true);
        JSONObject returningObject = new JSONObject();
        returningObject.put("dropoffPoints", optimal);
        returningObject.put("distances", RoutePlanningService.getDistances(optimal));

        String returningString = returningObject.toString(4);
        httpExchange.sendResponseHeaders(200, returningString.getBytes("UTF-8").length);
        httpExchange.getResponseBody().write(returningString.getBytes());
        httpExchange.getResponseBody().close();
        System.out.println(returningString);
        System.out.println("Load Balancing Server Running!");
    }
}
