package kmeans;

import java.util.ArrayList;
import java.util.List;

public class TestPoints {

	public List<SimplifiedTMSOrder> getPoints() {
		String ptString = "lat = 31.340183, lng = 121.604979," + "lat = 31.103835, lng = 121.178695,"
				+ "lat = 31.189356, lng = 121.443522," + "lat = 31.241829, lng = 121.504881,"
				+ "lat = 31.240944, lng = 121.481905," + "lat = 31.307551, lng = 121.52178,"
				+ "lat = 31.23893, lng = 121.388148," + "lat = 31.277828, lng = 121.563706,"
				+ "lat = 31.277749, lng = 121.563742," + "lat = 31.233085, lng = 121.461902,"
				+ "lat = 31.340183, lng = 121.604979," + "lat = 31.199448, lng = 121.443321,"
				+ "lat = 31.010447, lng = 121.241106," + "lat = 31.241055, lng = 121.480215,"
				+ "lat = 31.351451, lng = 121.315579," + "lat = 31.241762, lng = 121.50497,"
				+ "lat = 31.207251, lng = 121.407122," + "lat = 31.224433, lng = 121.422773,"
				+ "lat = 31.20127, lng = 121.446789," + "lat = 30.981884, lng = 121.873161,"
				+ "lat = 31.200224, lng = 121.316463," + "lat = 31.189356, lng = 121.443522,"
				+ "lat = 31.226481, lng = 121.357727," + "lat = 31.276016, lng = 121.485312,"
				+ "lat = 31.306452, lng = 121.522566," + "lat = 31.248872, lng = 121.463229,"
				+ "lat = 31.303107, lng = 121.334466";
		String[] pts = ptString.split(",");
		List<SimplifiedTMSOrder> retList = new ArrayList<SimplifiedTMSOrder>();
		for (int i = 0; i < pts.length; i += 2) {
			retList.add(new SimplifiedTMSOrder(
					new LatLon(Double.valueOf(pts[i].substring(6)) * 1000 - 30500,
							Double.valueOf(pts[i + 1].substring(7)) * 1000 - 121000)));
		}
		return retList;
	}
}
