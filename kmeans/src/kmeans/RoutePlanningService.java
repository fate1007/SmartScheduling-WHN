package kmeans;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

public class RoutePlanningService {

	public int[][] connectionMatrix;
	public double[][] weightMatrix;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		long startTime = System.currentTimeMillis();
		List<SimplifiedTMSOrder> points = new ArrayList<>();
		RoutePlanningService rps = new RoutePlanningService();
		// TestPoints tp = new TestPoints();
		// points = tp.getPoints();
		for (int i = 0; i < 30; i++) {
			Random random = new Random();
			SimplifiedTMSOrder newPoinTmsOrder = new SimplifiedTMSOrder(
					new LatLon((double) random.nextInt(1300) + 50., (double) random.nextInt(600) + 50.));
			newPoinTmsOrder.setVolume(random.nextDouble() / 2 + 0.5);
			if (i % 10 == 0) {
				SimplifiedTMSOrder newPoinTmsOrder1 = new SimplifiedTMSOrder(newPoinTmsOrder.getLatlon());
				newPoinTmsOrder1.setVolume(random.nextDouble() / 2 + 0.5);
				newPoinTmsOrder1.setAddressId(String.valueOf(i));
				points.add(newPoinTmsOrder1);
			}
			newPoinTmsOrder.setAddressId(String.valueOf(i));
			points.add(newPoinTmsOrder);
		}
		for (int z = 0; z < 100; z++) {
			// List<Cluster> result = rps.mainCluster(points, 10, 5, 7.);
			List<List<SimplifiedTMSOrder>> resultPoints = rps.clusterInterface(points, 10, 5, 7., 0);
			List<Cluster> result = new ArrayList<>();
			for (int i = 0; i < resultPoints.size(); i++) {
				result.add(new Cluster(resultPoints.get(i)));
			}
			long endTime = System.currentTimeMillis();
			// Displayer displayer = new Displayer(new
			// ArrayList<Cluster>(result), rps.connectionMatrix, "final", null);
			for (int i = 0; i < result.size(); i++) {
				System.out.printf("%d ", result.get(i).getPoints().size());
				if (i % 10 == 9) {
					System.out.println();
				}
			}
			for (int i = 0; i < result.size(); i++) {
				result.get(i).updateTotalVolume();
				System.out.printf("%s ", result.get(i).getPoints().get(0).getAddressId());
				if (i % 10 == 9) {
					System.out.println();
				}
			}
			for (int i = 0; i < result.size(); i++) {
				result.get(i).updateTotalVolume();
				System.out.printf("%f ", result.get(i).getTotalVolume());
				if (i % 10 == 9) {
					System.out.println();
				}
			}
			System.out.printf("Total run time is %f\n", (double) (endTime - startTime) / 1000);
		}
	}

	/**
	 * @brief Main callback for get points clustered
	 * 
	 * @param points:
	 *            all order locations
	 * @param maxTour,
	 *            minTour: limitation on orders per driver
	 * @param volumeLimit:
	 *            volume limitation per driver
	 * @param speed:
	 *            estimated speed for each car
	 */
	public List<List<SimplifiedTMSOrder>> clusterInterface(List<SimplifiedTMSOrder> points, int maxTour, int minTour,
			double volumeLimit, double speed) {
		// for return
		List<List<SimplifiedTMSOrder>> result = new ArrayList<>();
		// for function return reception
		List<Cluster> resultClusters = new ArrayList<>();
		// cluster func
		resultClusters = mainCluster(init(points), maxTour, minTour, volumeLimit);
		// for removal
		List<SimplifiedTMSOrder> removaList = new ArrayList<>(points);
		for (int i = 0; i < resultClusters.size(); i++) {
			List<SimplifiedTMSOrder> tempList = resultClusters.get(i).getPoints();
			List<SimplifiedTMSOrder> toAdd = new ArrayList<>();
			for (SimplifiedTMSOrder s : tempList) {
				for (int d = 0; d < s.getDuplicate(); d++) {
					for (int p = 0; p < removaList.size(); p++) {
						if (removaList.get(p).getLatlon().equals(s.getLatlon())) {
							toAdd.add(removaList.remove(p));
							break;
						}
					}
				}
			}
			result.add(toAdd);
		}
		// for (int i = 0; i < result.size() - 1; i++) {
		// for (int j = i + 1; j < result.size(); j++) {
		// for (int i1 = 0; i1 < result.get(i).size(); i1++) {
		// for (int j1 = 0; j1 < result.get(j).size(); j1++) {
		// if
		// (result.get(i).get(i1).getLatlon().equals(result.get(j).get(j1).getLatlon()))
		// {
		// System.out.println("fuck!");
		// }
		// }
		// }
		// }
		// }
		return result;
	}

	// get unique points array
	private List<SimplifiedTMSOrder> init(List<SimplifiedTMSOrder> points){
		List<SimplifiedTMSOrder> retList = new ArrayList<>();
		// sort in order to transfer info later
		points.sort(new Comparator<SimplifiedTMSOrder>() {
			@Override
			public int compare(SimplifiedTMSOrder s1, SimplifiedTMSOrder s2) {
				if (s1.getLatlon().getLat() > s2.getLatlon().getLat())
					return 1;
				else if (s1.getLatlon().getLat() < s2.getLatlon().getLat())
					return -1;
				else {
					if (s1.getLatlon().getLon() > s2.getLatlon().getLon())
						return 1;
					else if (s1.getLatlon().getLon() < s2.getLatlon().getLon())
						return -1;
					else
						return 0;
				}
			}
		});
		retList.add(new SimplifiedTMSOrder(points.get(0).getLatlon(), points.get(0).getVolume()));
		for (int i = 1; i < points.size(); i++) {
			if (points.get(i).getLatlon().equals(points.get(i - 1).getLatlon())) {
				retList.get(retList.size() - 1).increaseDuplicate();
				continue;
			}
			retList.add(new SimplifiedTMSOrder(points.get(i).getLatlon(), points.get(i).getVolume()));
		}
		return retList;
	}

	/** @brief Main cluster controls all work flow
     * 
     * @param DropOffPoints: orders
     * @param maxTour, minTour: limitation on orders per driver
     * @param volumeLimit: volume limitation per car
     */
	public List<Cluster> mainCluster(List<SimplifiedTMSOrder> DropOffPoints, int maxTour,
			int minTour, double volumeLimit) {
		List<List<SimplifiedTMSOrder>> result = new ArrayList<>();
		List<Cluster> clusters = new ArrayList<>();
		boolean breakFlag = false;
		int reclusterIter = 0;

		// if has volume constraint, use it to get original cluster number
		int clusterNumber;
		if (volumeLimit > 0) {
			clusterNumber = (int) Math.ceil(getTotalVolume(DropOffPoints) / volumeLimit);
			// System.out.println(getTotalVolume(DropOffPoints));
		} else {
			clusterNumber = (int) Math
					.ceil((double) getTotalNumPoints(DropOffPoints) / (double) ((maxTour + minTour) / 2));
			// clusterNumber = (int) Math.ceil(DropOffPoints.size() / maxTour);
		}

		// for test
		// clusterNumber = 10;

		while (!breakFlag) {
			// use kmeans to cluster
			result = kmeansCluster(DropOffPoints, clusterNumber);
			// initiate each cluster
			clusters = new ArrayList<>();
			for (List<SimplifiedTMSOrder> singleCluster : result) {
				clusters.add(new Cluster(singleCluster));
			}
			// Displayer displayer = new Displayer(clusters, connectionMatrix,
			// "kmeans", null);
			//
			// if (!breakFlag)
			// break;

			// get each cluster's radius and center
			// get each cluster's status (more or less or at boundary or good)
			for (int i = 0; i < clusters.size(); i++) {
				Cluster currentCluster = clusters.get(i);
				currentCluster.updateCenter();
				currentCluster.updateRadius();
				currentCluster.updateStatus(minTour, maxTour);
			}
			ClusterAdapter clusterAdapter = new ClusterAdapter(clusters, minTour, maxTour, volumeLimit);
			// get connection matrix
			connectionMatrix = clusterAdapter.getConnectionMatrix();
			// get weight matrix
			weightMatrix = clusterAdapter.getWeightMatrix();

			// test for dijkstra
			// int len = connectionMatrix.length;
			// for (int i = 0; i < 10; i++) {
			// Random random = new Random();
			// int from = random.nextInt(len);
			// int to = random.nextInt(len);
			// List<Integer> path = clusterAdapter.dijkstra(connectionMatrix,
			// weightMatrix, from, to);
			// Displayer displayer = new Displayer(clusterAdapter.getClusters(),
			// connectionMatrix, String.valueOf(i),
			// path);
			// }
			// if (breakFlag) {
			// break;
			// }
			// swap based on minTour & maxTour
			breakFlag = true;
			clusterAdapter.swapProcessing();
			for (int i = 0; i < clusters.size(); i++) {
				int numPoint = clusters.get(i).getNumPoints();
				if (numPoint > maxTour || numPoint < minTour) {
					breakFlag = false;
				}
			}
			// Must update volume info first
			for (int i = 0; i < clusters.size(); i++) {
				clusters.get(i).updateTotalVolume();
				// System.out.printf("%f ", clusters.get(i).getTotalVolume());
				// if (i % 10 == 9) {
				// System.out.println();
				// }
			}

			// update volumes if its a constraint
			if (volumeLimit > 0) {
				breakFlag = false;
				while (!breakFlag) {
					breakFlag = true;
					clusterAdapter.swapVolumeProcessing();
				
					// if satisfied
					for (int i = 0; i < clusters.size(); i++) {
						int numPoint = clusters.get(i).getNumPoints();
						if (clusters.get(i).getTotalVolume() > volumeLimit) {
							// System.out.println("Not
							// suitable!Rearranging...");
							breakFlag = false;
							break;
						}
					}

					if (++reclusterIter % 10 == 0) {
						reclusterIter = 0;
						clusterNumber++;
						// System.out.println("Need more
						// cluster!Rearranging...");
						break;
					}
				}
			}
		}

		return clusters;
	}

	/** @brief Get total volume of each car
     * 
     * @param DropOffPoints: orders
     */
	public double getTotalVolume(List<SimplifiedTMSOrder> DropOffPoints) {
		double totalVolume = 0.;
		for (SimplifiedTMSOrder stmsOrder : DropOffPoints) {
			totalVolume += stmsOrder.getVolume();
		}
		return totalVolume;
	}
	
	public int getTotalNumPoints(List<SimplifiedTMSOrder> DropOffPoints) {
		int totalNum = 0;
		for (SimplifiedTMSOrder stmsOrder : DropOffPoints) {
			totalNum += stmsOrder.getDuplicate();
		}
		return totalNum;
	}

	/**
	 * @brief Kmeans algo from existing jar file associate with orders' info
	 * 
	 * @param pts
	 * @param numClusters
	 */
	private List<List<SimplifiedTMSOrder>> kmeansCluster(List<SimplifiedTMSOrder> pts, int numClusters) {
		Dataset pointsSet = new DefaultDataset();
		List<List<SimplifiedTMSOrder>> clusterOfPoints = new ArrayList<>();
		List<SimplifiedTMSOrder> temp = new ArrayList<>(pts);

		for (SimplifiedTMSOrder pt : pts) {
			Instance curInstance = new DenseInstance(new double[] { pt.getLatlon().getLat(), pt.getLatlon().getLon() }, 
					pt.getAddressId());
			pointsSet.add(curInstance);
		}

		KMeans clusterer = new KMeans(numClusters);
		Dataset[] pointsClusters = clusterer.cluster(pointsSet);
		for (Dataset ds : pointsClusters) {
			List<SimplifiedTMSOrder> lst = new ArrayList<SimplifiedTMSOrder>();
			for (Instance ins : ds) {
				SimplifiedTMSOrder stms = new SimplifiedTMSOrder(new LatLon(ins.get(0), ins.get(1)), 0);
				for (int i = 0; i < temp.size(); i++) {
					SimplifiedTMSOrder pt = temp.get(i);
					if (pt.getLatlon().equals(stms.getLatlon())) {
						// stms.setAddressId(pt.getAddressId());
						stms.setVolume(pt.getVolume());
						stms.setDuplicate(pt.getDuplicate());
						// temp.remove(i);
						break;
					}
				}
				lst.add(stms);
			}
			clusterOfPoints.add(lst);
		}
		return clusterOfPoints;
	}
}
