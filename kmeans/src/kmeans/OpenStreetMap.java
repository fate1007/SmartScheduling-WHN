package kmeans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

/**shMap<>();
    
 * OpenStreetMap utilities
 * @author Kelvin Liu
 *
 */
public class OpenStreetMap {
    private static final String osmRoutePlanningAPI = "http://localhost:8989/route?point=%.6f,%.6f&point=%.6f,%.6f&calc_points=false";
	private static Map<StartOriginPair, Integer> distanceMap = new HashMap<>();
	private static Set<LatLon> LLSet = new HashSet<>();

    public static final int getDrivingDistance(LatLon origin, LatLon destination) throws Exception {
        StartOriginPair sop = new StartOriginPair(origin, destination);
        if (distanceMap.containsKey(sop))
            return distanceMap.get(sop);
//        if (!ConversionTest.monitored.contains(origin)) {
////            System.out.println(String.format("Fuck!!! %s, %s", origin.getLat(), origin.getLon()));
////            new Exception().printStackTrace();
//        }
//
//        if (!ConversionTest.monitored.contains(destination)) {
////            System.out.println(String.format("Fuck!!! %s, %s", destination.getLat(), destination.getLon()));
////            new Exception().printStackTrace();
//        }
		LLSet.add(origin);
		LLSet.add(destination);

        URL queryURL = new URL(String.format(osmRoutePlanningAPI, origin.getLat(), origin.getLon(), destination.getLat(),
                destination.getLon()));
        BufferedReader queryurlReader = new BufferedReader(new InputStreamReader(queryURL.openStream()));
        String line = "";
        String totalLine = "";
        while ((line = queryurlReader.readLine()) != null) {
            totalLine += line;
        }

        JSONObject resultObj = new JSONObject(totalLine);
        JSONObject pathsObj = resultObj.getJSONArray("paths").getJSONObject(0);
        double distance = pathsObj.getDouble("distance");
        int finalReturningDist = (int) Math.round(distance);
        synchronized (distanceMap) {
            distanceMap.put(new StartOriginPair(origin, destination), finalReturningDist);
        }
		if (distanceMap.size() % 1000 == 0) {
			int temp = LLSet.size();
			System.out.println("Distance map's size is: " + distanceMap.size());
			System.out.println("LatLon Set size is: " + temp);
		}

		return finalReturningDist;
    }

    private static class StartOriginPair {
        private LatLon origin;
        private LatLon destination;
        private StartOriginPair(LatLon origin, LatLon destination) {
            this.origin = origin;
            this.destination = destination;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((destination == null) ? 0 : destination.hashCode());
            result = prime * result + ((origin == null) ? 0 : origin.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StartOriginPair other = (StartOriginPair) obj;
            if (destination == null) {
                if (other.destination != null)
                    return false;
            } else if (!destination.equals(other.destination))
                return false;
            if (origin == null) {
                if (other.origin != null)
                    return false;
            } else if (!origin.equals(other.origin))
                return false;
            return true;
        }
    }

    private static boolean testOSMConnection() {
        String host = "localhost";
        int port = 8989;
        int timeout = 4000;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    public static void configureCalculationSchema() {
        boolean result = testOSMConnection();
        if (result)
            DropoffPointsDistanceMeasure.enableServerAccess();
        else
            DropoffPointsDistanceMeasure.disableServerAccess();
    }
}
