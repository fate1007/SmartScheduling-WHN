package kmeans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClusterAdapter {

	private List<Cluster> clusters;

	public List<Cluster> getClusters() {
		return clusters;
	}

	private int[][] connectionMatrix;

	private double[][] weightMatrix;

	private int[][] swappingmatrix;

	private int minTour;

	private int maxTour;

	private double volumeLimit;

	public ClusterAdapter(List<Cluster> clusters, int minTour, int maxTour, double volumeLimit) {
		this.clusters = clusters;
		weightMatrix = new double[clusters.size()][clusters.size()];
		swappingmatrix = new int[clusters.size()][clusters.size()];
		this.maxTour = maxTour;
		this.minTour = minTour;
		this.volumeLimit = volumeLimit;
	}

	/**
	 * @brief Get the connection Matrix, a cluster doesn't connect to itself,
	 *        only initiate once!
	 */
	public int[][] getConnectionMatrix() {

			connectionMatrix = new int[clusters.size()][clusters.size()];
			for (int i = 0; i < clusters.size(); i++) {
				List<Cluster> clustersSortedByDistance = getClustersSortedByDistance(clusters.get(i));
			List<List<Double>> ranges1 = new ArrayList<>();
				for (int j = 1; j < clustersSortedByDistance.size(); j++) {
					Cluster currentCluster = clustersSortedByDistance.get(j);
					int curIndex = 0;
					// find index of current cluster in connectionMatrix
					for (; curIndex < clusters.size(); curIndex++) {
						if (clusters.get(curIndex) == currentCluster) {
							break;
						}
					}
				if (isConnected(clusters.get(i), currentCluster, ranges1)) {
						connectionMatrix[i][curIndex] = 1;
						connectionMatrix[curIndex][i] = 1;
					}
				}
			}
		return connectionMatrix;
	}

	/**
	 * @brief sort all clusters by their center's distance to center cluster's
	 *        center
	 * 
	 * @param center
	 */
	private List<Cluster> getClustersSortedByDistance(final Cluster center) {
		List<Cluster> result = new ArrayList<Cluster>(this.clusters);
		double[] distances = new double[clusters.size()];
		for (int i = 0; i < clusters.size(); i++) {
			distances[i] = DropoffPointsDistanceMeasure.measurePoint(clusters.get(i).getCenter(), center.getCenter());
		}
		sort(distances, result, 0, result.size() - 1);
		// result.sort(new Comparator<Cluster>() {
		// @Override
		// public int compare(Cluster c1, Cluster c2) {
		// double distance1 =
		// DropoffPointsDistanceMeasure.measurePoint(center.getCenter(),
		// c1.getCenter());
		// double distance2 =
		// DropoffPointsDistanceMeasure.measurePoint(center.getCenter(),
		// c2.getCenter());
		// if (distance1 > distance2) {
		// return 1;
		// } else if (distance1 < distance2) {
		// return -1;
		// } else {
		// return 0;
		// }
		// }
		// });
		return result;
	}

	// quicksort
	private void sort(double[] sortedByDistance, List<Cluster> c, int start, int end) {
		int i = start;
		int j = end;
		if (j <= i) {
			return;
		}
		if (j - i == 1) {
			if (sortedByDistance[i] > sortedByDistance[j]) {
				swap(sortedByDistance, c, i, j);
			}
			return;
		}
		double mid = sortedByDistance[start];
		while(i<j){
			while (sortedByDistance[i] <= mid) {
				i++;
				if (i >= c.size()) {
					break;
				}
			}
			while (sortedByDistance[j] > mid) {
				j--;
				if (j < 0) {
					break;
				}
			}
			if (i >= j) {
				break;
			}
			if (i < j)
				swap(sortedByDistance, c, i, j);
		}
		swap(sortedByDistance, c, start, j);
		sort(sortedByDistance, c, start, j - 1);
		sort(sortedByDistance, c, j + 1, end);
	}

	// swap for quicksort
	private void swap(double[] sortedByDistance, List<Cluster> c, int i, int j) {
		// Displayer displayer1Displayer = new Displayer(c, connectionMatrix,
		// "swap0" + String.valueOf(i) + String.valueOf(j));
		double temp = sortedByDistance[i];
		sortedByDistance[i] = sortedByDistance[j];
		sortedByDistance[j] = temp;
		Cluster tempCluster = c.get(i);
		c.set(i, c.get(j));
		c.set(j, tempCluster);
		// Displayer displayer2Displayer = new Displayer(c, connectionMatrix,
		// "swap1");
	}

	/** @brief If two cluster are directly projected to each other we see them as connected (nearby cluster)
     * 
     * @param centerCluster
     * @param other Clusters
     * @param ranges occupied of centerCluster
     */
	private boolean isConnected(Cluster centerCluster, Cluster other, List<List<Double>> ranges) {
		List<SimplifiedTMSOrder> points = other.getPoints();
		int len = points.size();
		double[] angles = new double[len];
		double centerAngle;
		LatLon center = centerCluster.getCenter();

		// version 1
		// get angles of each point in currentcluster with respect to original
		// cluster's center
		 for (int i = 0; i < len; i++) {
		 // get vector
			LatLon currentPointLatlon = points.get(i).getLatlon();
			double[] tempVector = new double[2];
			tempVector[0] = currentPointLatlon.getLat() - center.getLat();
			tempVector[1] = currentPointLatlon.getLon() - center.getLon();

			// norm vector
			tempVector = norm(tempVector);

			// get angle with respect with x axis
			if (tempVector[1] >= 0) {
				angles[i] = Math.acos(tempVector[0]);
			} else {
				angles[i] = 2 * Math.PI - Math.acos(tempVector[0]);
			}
		 }
		// get center vector to see if collide
		double[] centerVector = new double[2];
		centerVector[0] = other.getCenter().getLat() - center.getLat();
		centerVector[1] = other.getCenter().getLon() - center.getLon();
		centerVector = norm(centerVector);
		if (centerVector[1] >= 0) {
			centerAngle = Math.acos(centerVector[0]);
		} else {
			centerAngle = 2 * Math.PI - Math.acos(centerVector[0]);
		}

		Arrays.sort(angles);
		// double range = angles[len-1] - angles[0];
		double range = angles[angles.length - 1] - angles[0];
		if (range >= Math.PI) {
			int ind = 0;
		    for(ind=0;ind<len;ind++){
		    	if(angles[ind] > Math.PI){
		    		break;
		    	}
		    }
			double[] angleFinalRange = { angles[0], angles[ind - 1] };
			double[] angleFinalRangeLeft = { angles[ind], angles[angles.length - 1] };
			return !isCollide(ranges, centerAngle)
					&& (hasCoverage(angleFinalRange, ranges, 0) + hasCoverage(angleFinalRangeLeft, ranges, 0) > Math.PI
							* 0.1);
		}
		return !isCollide(ranges, centerAngle)
				&& hasCoverage(
				new double[] { angles[0], angles[angles.length - 1] }, ranges, 0) > Math.PI * 0.1;
	}

	// norm a vector
	private double[] norm(double[] tempVector) {
		double len = Math.sqrt(Math.pow(tempVector[0], 2) + Math.pow(tempVector[1], 2));
		tempVector[0] /= len;
		tempVector[1] /= len;
		return tempVector;
	}

	// If the cluster collide to former cluster
	private boolean isCollide(List<List<Double>> ranges, double centerAngle) {
		for (int i = 0; i < ranges.size(); i++) {
			if (centerAngle >= ranges.get(i).get(0) && centerAngle <= ranges.get(i).get(1)) {
				return true;
			}
		}
		return false;
	}


	// if current cluster has valid projection on center cluster
	private double hasCoverage(double[] range, List<List<Double>> ranges, int startIndex) {
		for (int i = startIndex; i < ranges.size(); i++) {
			List<Double> currRange = ranges.get(i);
			double left = currRange.get(0);
			double right = currRange.get(1);

			// stored range inside current range, blocked
			if (range[0] < left && range[1] > right) {
				currRange.set(0, range[0]);
				currRange.set(1, range[1]);
				return 0.;
			}
			// not intersected, continue to next range
			else if (range[0] >= right || range[1] <= left) {
				if (i == ranges.size()) {
					ranges.add(Arrays.asList(range[0], range[1]));
					return range[0] - range[1];
				}
				return hasCoverage(range, ranges, i + 1);
			}
			// totally inside stored range
			else if (range[0] >= left && range[1] <= right) {
				return 0.;
			}
			// patially intersect
			else {
				if (range[0] < left) {
					currRange.set(0, range[0]);
					return hasCoverage(new double[] { range[0], left }, ranges, i + 1);
				}
				if (range[1] > right) {
					currRange.set(1, range[1]);
					return hasCoverage(new double[] { right, range[1] }, ranges, i + 1);
				}
			}
		}
		ranges.add(Arrays.asList(range[0], range[1]));
		return range[1] - range[0];
	}

	/**
	 * @brief Get Weight Matrix
	 */
	public double[][] getWeightMatrix() {
		if (connectionMatrix == null) {
			getConnectionMatrix();
		}
		int len = connectionMatrix.length;
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < len; j++) {
				if (connectionMatrix[i][j] == 1) {
					weightMatrix[i][j] = getWeightBetweenTwoCluster(i, j);
				}
			}
		}
		return this.weightMatrix;
	}

	// return weighted distance as weight
	private double getWeightBetweenTwoCluster(int f, int t) {
		int returnIndex = -1;
		double weightedDistance = Double.POSITIVE_INFINITY;
		Cluster from = clusters.get(f);
		Cluster to = clusters.get(t);
		List<SimplifiedTMSOrder> points = from.getPoints();
		LatLon centerTo = to.getCenter();
		LatLon centerFrom = from.getCenter();
		for (int i = 0; i < points.size(); i++) {
			if (points.get(i).getDuplicate() > 1)
				continue;
			double tempWeightedDistance = Math
					.abs(DropoffPointsDistanceMeasure.measurePoint(centerFrom, points.get(i).getLatlon())
							- from.getRadius());
			tempWeightedDistance *= tempWeightedDistance;
			tempWeightedDistance += Math
					.pow(DropoffPointsDistanceMeasure.measurePoint(centerTo, points.get(i).getLatlon()), 2);
			if (tempWeightedDistance < weightedDistance) {
				returnIndex = i;
				weightedDistance = tempWeightedDistance;
			}
		}
		swappingmatrix[f][t] = returnIndex;
		return weightedDistance;
	}

	public void swapProcessing() {
		int moreOrders = 0;
		int lessOrders = 0;
		int len = clusters.size();
		for (int i = 0; i < len; i++) {
			// % 0 : stands for cluster whose number of nodes between (min, max)
			// % 1 : stands for the number of node is one cluster is larger than
			// maximum
			// % 2: stands for the number of node is one cluster is smaller than
			// minimum
			// % 3: stands for the number of node is one cluster is equal to
			// minimum
			// % 4: stands for the number of node is one cluster is equal to
			// maximum
			int status = clusters.get(i).getStatus();
			if (status == 1) {
				moreOrders += clusters.get(i).getNumPoints() - maxTour;
			}
			if (status == 2) {
				lessOrders += minTour - clusters.get(i).getNumPoints();
			}
		}
		if (moreOrders > lessOrders) {
			swapProcessingMoreDominate();
			swapProcessingLessDominate();
		} else {
			swapProcessingLessDominate();
			swapProcessingMoreDominate();
		}
	}

	// number of clusters with extended orders more than with less orders
	private void swapProcessingMoreDominate() {
		// System.out.println("Start swap processing more dominate...");
		List<Integer> fromClusters = new ArrayList<>();
		List<Integer> toClusters = new ArrayList<>();
		int len = clusters.size();

		// get from and to clusters
		for (int i = 0; i < len; i++) {
			int status = clusters.get(i).getStatus();
			if (status == 0 || status == 2 || status == 3) {
				toClusters.add(i);
			}
			if (status == 1) {
				fromClusters.add(i);
			}
		}

		// use dijkstra for swapping
		for (int i = 0; i < fromClusters.size(); i++) {
			Cluster currentSourceCluster = clusters.get(fromClusters.get(i));
			int numExtended = currentSourceCluster.getNumPoints() - maxTour;
			for (int j = 0; j < numExtended; j++) {
				int toClusterIndex = 0;
				double cost = Double.MAX_VALUE;
				List<Integer> swappingPath = new ArrayList<>();

				// get shortest swapping path
				for (int k = 0; k < toClusters.size(); k++) {
					Cluster currentdestCluster = clusters.get(toClusters.get(k));
					// System.out.printf("from %d to %d\n", fromClusters.get(i),
					// toClusters.get(k));
					List<Integer> currentPath = dijkstra(connectionMatrix, weightMatrix, fromClusters.get(i),
							toClusters.get(k));
					// failed to reach destination cluster
					if (currentPath == null || currentPath.size() < 2) {
						continue;
					}
					double tempCost = getCost(currentPath);
					if (tempCost < cost) {
						cost = tempCost;
						swappingPath = currentPath;
						toClusterIndex = k;
					}
				}

				if (swappingPath.size() < 2) {
					break;
				}
				// swap through path and remove the destination cluster if it's
				// full
				if (swappingThroughPath(swappingPath, 0)) {
					toClusters.remove(toClusterIndex);
				}
			}
		}
	}

	// number of clusters with less orders more than with extended orders
	private void swapProcessingLessDominate() {
		// System.out.println("Start swap processing less dominate...");
		List<Integer> fromClusters = new ArrayList<>();
		List<Integer> toClusters = new ArrayList<>();
		int len = clusters.size();

		// get from and to clusters
		for (int i = 0; i < len; i++) {
			int status = clusters.get(i).getStatus();
			if (status == 2) {
				toClusters.add(i);
			}
			if (status == 1 || status == 0 || status == 4) {
				fromClusters.add(i);
			}
		}

		// use dijkstra for swapping
		for (int i = 0; i < toClusters.size(); i++) {
			Cluster currentDestinationCluster = clusters.get(toClusters.get(i));
			int numNeeded = minTour - currentDestinationCluster.getNumPoints();
			for (int j = 0; j < numNeeded; j++) {
				int fromClusterIndex = 0;
				double cost = Double.MAX_VALUE;
				List<Integer> swappingPath = new ArrayList<>();

				// get shortest swapping path
				for (int k = 0; k < fromClusters.size(); k++) {
					Cluster currentSourceCluster = clusters.get(fromClusters.get(k));
					List<Integer> currentPath = dijkstra(connectionMatrix, weightMatrix, fromClusters.get(k),
							toClusters.get(i));
					// failed to reach destination cluster
					if (currentPath == null || currentPath.size() < 2) {
						continue;
					}
					double tempCost = getCost(currentPath);
					if (tempCost < cost) {
						cost = tempCost;
						swappingPath = currentPath;
						fromClusterIndex = k;
					}
				}

				if (swappingPath.size() < 2) {
					break;
				}
				// swap through path and remove the destination cluster if it's
				// full
				if (swappingThroughPath(swappingPath, 1)) {
					fromClusters.remove(fromClusterIndex);
				}
			}
		}
	}

	/**
	 * @param connectionMatrix
	 * @param weightMatrix
	 * @param from
	 * @param to
	 * @return
	 */
	public List<Integer> dijkstra(int[][] connectionMatrix, double[][] weightMatrix, int from, int to) {
		// System.out.println("Start dijkstra...");
		int len = connectionMatrix.length;
		double[] dist = new double[len];
		int[] prev = new int[len];
		Set<Integer> uSet = new HashSet<>();
		List<Integer> path = new ArrayList<>();

		// init
		for (int i = 0; i < len; i++) {
			dist[i] = Double.POSITIVE_INFINITY;
		}
		dist[from] = 0;
		uSet.add(from);

		// spread & update dist to each point until all nodes are visited
		while (uSet.size() != len) {
			double tempDist = Double.POSITIVE_INFINITY;
			int nearestNode = -1;
			// update dist, prev node and select the nearest node add to uSet
			for (int i : uSet) {
				for (int j = 0; j < len; j++) {
					if (connectionMatrix[i][j] == 1 && !uSet.contains(j)) {
						if (weightMatrix[i][j] + dist[i] < dist[j]) {
							dist[j] = weightMatrix[i][j] + dist[i];
							prev[j] = i;
						}
						if (dist[j] < tempDist) {
							nearestNode = j;
							tempDist = dist[j];
						}
					}
				}
			}
			if (nearestNode == -1) {
				break;
			}
			uSet.add(nearestNode);
		}
		// System.out.println("Done searching.");

		// failed to reach destination cluster
		if (!uSet.contains(to)) {
			// Displayer displayer = new Displayer(clusters, connectionMatrix,
			// "fail", null);
			// try {
			// Thread.sleep(1000000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// System.exit(0);
			// System.out.println("didn't go through! Problem in
			// connectionMatrix.");
			// System.out.printf("from %d to %d\n", from, to);
			return null;
		}

		// get path
		int node = to;
		while (node != from) {
			path.add(node);
			node = prev[node];
		}
		path.add(from);
		Collections.reverse(path);

		return path;
	}

	private double getCost(List<Integer> path) {
		if (path == null) {
			return Double.POSITIVE_INFINITY;
		}
		int len = path.size();
		double ret = 0.;
		for (int i = 0; i < len - 1; i++) {
			ret += weightMatrix[path.get(i)][path.get(i + 1)];
		}
		return ret;
	}

	// swap with updating weight matrix & status
	// input lists are all index of cluster in this.clusters
	private boolean swappingThroughPath(List<Integer> path, int type) {
		// System.out.println("Start swapping through path...");
		// Displayer displayer1 = new Displayer(clusters, connectionMatrix,
		// "swap", path);
		// the last index
		if (path == null || path.size() < 1) {
			return false;
		}
		int len = path.size() - 1;
		for (int i = 0; i < len; i++) {
			Cluster fromCluster = clusters.get(path.get(i));
			Cluster toCluster = clusters.get(path.get(i + 1));

			// pass point
			int pointToSwapIndex = swappingmatrix[path.get(i)][path.get(i + 1)];
			SimplifiedTMSOrder pointToSwap = fromCluster.getPoints().remove(pointToSwapIndex);
			toCluster.getPoints().add(pointToSwap);

		}
		// Displayer displayer2 = new Displayer(clusters, connectionMatrix,
		// "swap", path);
		getWeightMatrix();
		// System.out.println("Done swapping through path.");
		// update status, only need to update from and to cluster whose points
		// number changed
		// 'type': 0:more dominate; 1: less dominate; 2: volumewise
		if (type == 0) {
			clusters.get(path.get(0)).updateStatus(minTour, maxTour);
			clusters.get(path.get(len)).updateStatus(minTour, maxTour);
			return clusters.get(path.get(len)).getStatus() == 4 || clusters.get(path.get(len)).getStatus() == 1;
		}
		else if (type == 1) {
			clusters.get(path.get(len)).updateStatus(minTour, maxTour);
			clusters.get(path.get(0)).updateStatus(minTour, maxTour);
			return clusters.get(path.get(0)).getStatus() == 3 || clusters.get(path.get(0)).getStatus() == 2;
		}
		else {
			clusters.get(path.get(len)).updateStatus(minTour, maxTour);
			clusters.get(path.get(0)).updateStatus(minTour, maxTour);
			for (int i = 0; i < path.size(); i++) {
				clusters.get(path.get(i)).updateTotalVolume();
			}
			return false;
		}
	}

	// almost same as more dominate
	public void swapVolumeProcessing() {
		List<Integer> fromClusters = new ArrayList<>();
		List<Integer> toClusters = new ArrayList<>();
		int len = clusters.size();

		// get from and to clusters
		for (int i = 0; i < len; i++) {
			int status = clusters.get(i).getVolumeStatus(volumeLimit);
			if (status == 2) {
				fromClusters.add(i);
			}
		}

		// use dijkstraByVolumn for swapping
		for (int i = 0; i < fromClusters.size(); i++) {
			Cluster currentSourceCluster = clusters.get(fromClusters.get(i));
			while (currentSourceCluster.getTotalVolume() > volumeLimit) {
				double cost = Double.MAX_VALUE;
				List<Integer> swappingPath = new ArrayList<>();

				for (int t = 0; t < clusters.size(); t++) {
					if (clusters.get(t).getTotalVolume() + 1 < volumeLimit) {
						toClusters.add(t);
					}
				}
				// no any cluster to be receiver
				if (toClusters.size() == 0) {
					break;
				}

				// get shortest swapping path
				for (int k = 0; k < toClusters.size(); k++) {
					Cluster currentdestCluster = clusters.get(toClusters.get(k));
					List<Integer> currentPath = dijkstra(connectionMatrix, weightMatrix, fromClusters.get(i),
							toClusters.get(k));
					// todo ensure the first two cluster and dest cluster won't
					// exceed limit
					// if(!satisfied(currentPath)){
					// currentPath = null;
					// }

					double tempCost = getCost(currentPath);
					if (tempCost < cost) {
						cost = tempCost;
						swappingPath = currentPath;
					}
				}

				// cannot reach any receiver cluster, need reclustered
				if (swappingPath.size() < 1) {
					break;
				}

				// swap through path
				swappingThroughPath(swappingPath, 2);
			}
		}
	}
}
