package Util;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by Kelvin Liu on 08/07/2016.
 */
public class BaiduMapQuery {
    private static final String queryURL = "http://%s:1031/route?origin=%s,%s&destination=%s,%s";
    private static String[] serverList = {
        "115.159.161.232", "182.254.158.152",
            "115.159.145.14", "182.254.146.54",
            "115.159.62.65"
    };

    public double[] getBaiduDistanceAndTime(LatLon ll1, LatLon ll2) throws IOException {
        URL realURL = new URL(String.format(queryURL, serverList[0], ll1.getLat(), ll1.getLon(), ll2.getLat(), ll2.getLon()));
        BufferedReader br = new BufferedReader(new InputStreamReader(realURL.openStream()));
        String line = "", totalLine = "";
        while ((line = br.readLine()) != null) {
            totalLine += line;
        }

        JSONObject returnedObj = new JSONObject(totalLine);
        double distance = returnedObj.getDouble("distance");
        double time = returnedObj.getDouble("time");
        return new double[]{distance, time};
    }
}
