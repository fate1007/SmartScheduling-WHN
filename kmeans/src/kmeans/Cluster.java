package kmeans;

import java.util.List;

public class Cluster {

	private LatLon center = null;

	private List<SimplifiedTMSOrder> points;

	private int numPoints = 0;

	private double radius = -1.;

	private int status = 0;

	private int volumeStatus = 0;

	private double totalVolume = 0.;

	public Cluster(List<SimplifiedTMSOrder> points) {
		this.points = points;
		if (points != null && points.size() > 0) {
			this.numPoints = points.size();
		} else {
			throw new IllegalArgumentException("Orders cluster should contains at least one order!");
		}
	}

	public List<SimplifiedTMSOrder> getPoints() {
		return points;
	}

	public int getNumPoints() {
		numPoints = 0;
		for (int i = 0; i < points.size(); i++) {
			numPoints += points.get(i).getDuplicate();
		}
		return this.numPoints;
	}

	public void updateRadius() {
		double r = 0.;
		double temp = 0.;
		for (SimplifiedTMSOrder stms : points) {
			// temp =
			// DropoffPointsDistanceMeasure.measurePoint(stms.getLatlon(),
			// center);
			temp = Math.sqrt(Math.pow(stms.getLatlon().getLat() - center.getLat(), 2)
					+ Math.pow(stms.getLatlon().getLon() - center.getLon(), 2));
			if (temp > r) {
				r = temp;
			}
		}
		this.radius = r;
	}

	public double getRadius() {
		return this.radius;
	}

	public void updateCenter() {
		double lat = 0.;
		double lon = 0.;
		for (SimplifiedTMSOrder stms : points) {
			lat += stms.getLatlon().getLat();
			lon += stms.getLatlon().getLon();
		}
		lat /= numPoints;
		lon /= numPoints;
		center = new LatLon(lat, lon);
	}

	public LatLon getCenter() {
		return this.center;
	}

	public void updateStatus(int minTour, int maxTour) {
		// % 0 : stands for cluster whose number of nodes between (min, max)
		// % 1 : stands for the number of node in one cluster is larger than
		// maximum
		// % 2: stands for the number of node in one cluster is smaller than
		// minimum
		// % 3: stands for the number of node in one cluster is equal to minimum
		// % 4: stands for the number of node in one cluster is equal to maximum
		int len = getNumPoints();
		if (len < maxTour && len > minTour) {
			this.status = 0;
		} else if (len > maxTour) {
			status = 1;
		} else if (len < minTour) {
			status = 2;
		} else if (len == minTour) {
			status = 3;
		} else {
			status = 4;
		}
	}

	public int getStatus() {
		return this.status;
	}

	public int getVolumeStatus(double volumeLimit) {
		// 0: proper volume
		// 1: at boundary
		// 2: exceed upper boundary, need to be adapted
		if (totalVolume < volumeLimit) {
			volumeStatus = 0;
		} else if (volumeStatus == volumeLimit) {
			volumeStatus = 1;
		} else {
			volumeStatus = 2;
		}
		return volumeStatus;
	}

	public void updateTotalVolume() {
		double tempVolume = 0.;
		for (int i = 0; i < points.size(); i++) {
			tempVolume += points.get(i).getVolume();
		}
		totalVolume = tempVolume;
	}

	public double getTotalVolume() {
		return totalVolume;
	}

	public void setTotalVolume(double totalVolume) {
		this.totalVolume = totalVolume;
	}

}
