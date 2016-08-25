package Algorithm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import Core.PerformanceMonitor;
import Util.*;

import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.clustering.KMedoids;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import org.json.JSONArray;
import org.json.JSONObject;

import static Util.FileUtil.saveClustersToFile;

/**
 * Service for load balancing and route planning.
 * (Where core algorithm lies.)
 *
 * @author Kelvin Liu
 */
public class RoutePlanningService {

    public static List<GeoPolygon> fences = new ArrayList<GeoPolygon>();
    public static List<BasePoint> allBasePoints = new ArrayList<BasePoint>();

    public static int populationSize = 256;
    public static int numGAIterations = 1000;
    private static final int methodCount = 8;
    private List<SimplifiedTMSOrder> allDropoffPoints;
    private List<GeoFence> allFences;
    private Map<BasePoint, List<SimplifiedTMSOrder>> bpMap;

    public RoutePlanningService(List<SimplifiedTMSOrder> allDropoffPoints) {
        allFences = new ArrayList<>();
        this.allDropoffPoints = allDropoffPoints;
    }

    // Bundled dropoff points
    private List<List<SimplifiedTMSOrder>> bundled = new ArrayList<>();
    private List<String> baiduAPIKeys = Arrays.asList(
            "dgYeMDzd9eFsfOFYo2mdApxMgO50dKi3",
            "y1NbsiYHBGSk0jLm439P1A5hkXdGpGSC",
            "kYz0CtG6dT4WjVK0Pnh8oLrGtKHs9GAK",
            "Kw9PkY6v44LQFo9DIokOIWXMQN9rG95d");

    public List<List<SimplifiedTMSOrder>> getOptimalPlan(int minTour, int maxTour, boolean heaviness,
                                                         SimplifiedTMSOrder customizedDepot,
                                                         boolean clusteringOnly) {
        TMSRoutePlan.restoreDepot();
        if (customizedDepot != null)
            TMSRoutePlan.configureDepot(customizedDepot);
        try {
            double mediumSize = (double) (minTour + maxTour) / 2.0;
            // TODO still have bugs
            PerformanceMonitor.startClustering();
            List<List<SimplifiedTMSOrder>> clusteredPoints = equalCluster(allDropoffPoints,
                    (int) Math.ceil((double) allDropoffPoints.size() / mediumSize), minTour, maxTour);

            if (clusteringOnly)
                return clusteredPoints;
            PerformanceMonitor.clusteringFinished();
            List<TMSRoutePlan> optimalCandidates = new ArrayList<>();

            PerformanceMonitor.startRunningGA();
            for (List<SimplifiedTMSOrder> singleCluster : clusteredPoints) {
                TMSRoutePlan optimal = doGA(singleCluster, singleCluster.size(), singleCluster.size(), heaviness, false);
                optimalCandidates.add(optimal);
                System.out.println("GA procedure finished; optimal plan cost: " + optimal.getRealTotalCost());
            }
            PerformanceMonitor.GAFinished();

            List<List<SimplifiedTMSOrder>> returning = new ArrayList<>();
            for (TMSRoutePlan optRP : optimalCandidates) {
                returning.add(optRP.getPoints());
            }
            FileUtil.saveResultsToFile(returning);
            return returning;
        } catch (InterruptedException e) {
            return null;
        }
    }

    public static List<Integer> getDistances(List<List<SimplifiedTMSOrder>> routes) {
        List<Integer> returnVal = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            returnVal.add(getRouteDrivingDistance(routes.get(i), TMSRoutePlan.depotLocation));
        }

        return returnVal;
    }

    private static int getRouteDrivingDistance(List<SimplifiedTMSOrder> wayPoints, SimplifiedTMSOrder depot) {
        int sumDistance = 0;
        LatLon depotLL = depot.getLatlon();
        LatLon firstWP = wayPoints.get(0).getLatlon();
        LatLon lastWP = wayPoints.get(wayPoints.size() - 1).getLatlon();
        for (int i = 1; i < wayPoints.size(); i++) {
            LatLon prev = wayPoints.get(i - 1).getLatlon();
            LatLon next = wayPoints.get(i).getLatlon();
            try {
                int curDistance = OpenStreetMap.getDrivingDistance(prev, next);
                sumDistance += curDistance;
            } catch (Exception e) {
                int curDistance = (int) Math.ceil(GeoPolygon.getEarthDistance(prev.getLat(),
                        prev.getLon(), next.getLat(), next.getLon()) * 1000);
                sumDistance += curDistance;
            }
        }

        try {
            int curDistance = OpenStreetMap.getDrivingDistance(depotLL, firstWP);
            sumDistance += curDistance;
        } catch (Exception e) {
            int curDistance = (int) Math.ceil(GeoPolygon.getEarthDistance(depotLL.getLat(),
                    depotLL.getLon(), firstWP.getLat(), firstWP.getLon()) * 1000);
            sumDistance += curDistance;
        }

        try {
            int curDistance = OpenStreetMap.getDrivingDistance(lastWP, depotLL);
            sumDistance += curDistance;
        } catch (Exception e) {
            int curDistance = (int) Math.ceil(GeoPolygon.getEarthDistance(lastWP.getLat(),
                    lastWP.getLon(), depotLL.getLat(), depotLL.getLon()) * 1000);
            sumDistance += curDistance;
        }

        return sumDistance;
    }

    private List<List<SimplifiedTMSOrder>> kmeansCluster(List<SimplifiedTMSOrder> pts, int numClusters) {
        Dataset pointsSet = new DefaultDataset();
        List<List<SimplifiedTMSOrder>> clusterOfPoints = new ArrayList<>();

        for (SimplifiedTMSOrder pt : pts) {
            Instance curInstance = new DenseInstance(new double[]{pt.getLatlon().getLat(), pt.getLatlon().getLon()},
                    pt.getOrderLabel());
            pointsSet.add(curInstance);
        }

        KMeans clusterer = new KMeans(numClusters);
        Dataset[] pointsClusters = clusterer.cluster(pointsSet);
        for (Dataset ds : pointsClusters) {
            List<SimplifiedTMSOrder> lst = new ArrayList<SimplifiedTMSOrder>();
            for (Instance ins : ds) {
                lst.add(new SimplifiedTMSOrder((TMSOrderLabel) ins.classValue(), new LatLon(ins.get(0), ins.get(1))));
            }
            clusterOfPoints.add(lst);
        }
        return clusterOfPoints;
    }

    public static SimplifiedTMSOrder electCentroid(List<SimplifiedTMSOrder> pointsInCluster) {
        double minTotalDist = Double.MAX_VALUE;
        SimplifiedTMSOrder clusterCenterReal = null;

        for (SimplifiedTMSOrder stms : pointsInCluster) {
            double curTotalDist = 0;
            for (SimplifiedTMSOrder stmsOther : pointsInCluster) {
                curTotalDist += DropoffPointsDistanceMeasure.measurePoint(stms.getLatlon(), stmsOther.getLatlon());
            }

            if (curTotalDist < minTotalDist) {
                minTotalDist = curTotalDist;
                clusterCenterReal = stms;
            }
        }

        return clusterCenterReal;
    }

    private List<List<SimplifiedTMSOrder>> equalCluster(List<SimplifiedTMSOrder> pts, int numClusters, int mini, int maxi) {
        OpenStreetMap.configureCalculationSchema();
        final int mediumSize = (int) Math.ceil((double) pts.size() / (double) numClusters);
        final Dataset pointsSet = new DefaultDataset();
        KMeans clusterer_ = new KMeans(numClusters, 10000);
//        final KMedoids clusterer = new KMedoids(numClusters, 800, new DropoffPointsDistanceMeasure());
        for (SimplifiedTMSOrder stmso : pts) {
            pointsSet.add(new DenseInstance(new double[]{stmso.getLatlon().getLat(), stmso.getLatlon().getLon()},
                    stmso.getOrderLabel()));
        }

        final List<Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>>> clusteredMaps = new ArrayList<>();
        List<Thread> threadPool = new ArrayList<>();
        final int[] iterationConstant = {0};
//        for (int i = 0; i < 10; i++) {
//            Thread workerThread = new Thread(() -> {
        for (int i1 = 0; i1 < 1000; i1++) {
            System.out.println("Before clustering: " + System.currentTimeMillis());
            Dataset[] clustered = clusterer_.cluster(pointsSet);
            System.out.println("After clustering: " + System.currentTimeMillis());
            List<SimplifiedTMSOrder> centroids = new ArrayList<>();

            // Stores such pairs: <SimplifiedTMSOrder, DistPair>
            // Where distPair contains the cluster center and distance to the cluster center+
            Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> partitionedMap = new HashMap<>();
            for (Dataset sto : clustered) {
                List<SimplifiedTMSOrder> pointsInCluster = new ArrayList<>();
                for (Instance ii : sto)
                    pointsInCluster.add(new SimplifiedTMSOrder((TMSOrderLabel) ii.classValue(),
                            new LatLon(ii.value(0), ii.value(1))));
                SimplifiedTMSOrder clusterCenterReal = electCentroid(pointsInCluster);
                centroids.add(clusterCenterReal);
                partitionedMap.put(clusterCenterReal, pointsInCluster);
            }

            final Map<SimplifiedTMSOrder, DistPair> closestDistMap = new HashMap<>();
            // Above: got k cluster centers by using kmeans++
            // Below: start assigning points to clusters. Put minDistance into map.
            for (SimplifiedTMSOrder groupingKey : centroids) {
                for (SimplifiedTMSOrder orderPt : partitionedMap.get(groupingKey)) {
                    closestDistMap.put(orderPt, new DistPair(groupingKey, DropoffPointsDistanceMeasure.measurePoint(
                            groupingKey.getLatlon(), orderPt.getLatlon())));
                }
            }

            // Sort all tms orders based on their distance to the closest centroid
            List<SimplifiedTMSOrder> orderedTMS = new ArrayList<>(closestDistMap.keySet());

            System.out.println("Before " + System.currentTimeMillis());
            partitionedMap = reallocatePoints(closestDistMap, orderedTMS, mediumSize, centroids);
            System.out.println("After " + System.currentTimeMillis());
            clusteredMaps.add(partitionedMap);
            System.out.println("Iteration Finished!!! " + iterationConstant[0]);
            iterationConstant[0]++;
        }
//            });
//
//            workerThread.start();
//            threadPool.add(workerThread);
//        }

        for (Thread th : threadPool) {
            try {
                th.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Map<Integer, Integer> spMap = new HashMap<>();
        // TODO need to cache results
        Collections.sort(clusteredMaps, new Comparator<Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>>>() {

            @Override
            public int compare(Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> o1,
                               Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> o2) {
                int o1SPSize = 0;
                int o2SPSize = 0;
                if (spMap.containsKey(o1.hashCode())) {
                    o1SPSize = spMap.get(o1.hashCode());
                } else {
                    List<List<SwapProposal>> o1Proposals = generateListSwapProposal(new ArrayList<>(o1.keySet()), o1);
                    o1SPSize = o1Proposals.size();
                    spMap.put(o1.hashCode(), o1SPSize);
                }

                if (spMap.containsKey(o2.hashCode())) {
                    o2SPSize = spMap.get(o2.hashCode());
                } else {
                    List<List<SwapProposal>> o2Proposals = generateListSwapProposal(new ArrayList<>(o2.keySet()), o2);
                    o2SPSize = o2Proposals.size();
                    spMap.put(o2.hashCode(), o2SPSize);
                }

                if (o2SPSize > o1SPSize)
                    return -1;
                if (o1SPSize > o2SPSize)
                    return 1;
                return 0;
            }

        });

        int bestSwappingProposals = Integer.MAX_VALUE;
        List<List<SimplifiedTMSOrder>> iteratedBest = null;
        for (int goodClustersIndex = 0; goodClustersIndex < 100; goodClustersIndex++) {
            Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> curEvaluating = clusteredMaps.get(goodClustersIndex);
            // Above锛� Finishing initialization

            List<List<SimplifiedTMSOrder>> intermediateResult = new ArrayList<>();
            for (SimplifiedTMSOrder orderSetKey : curEvaluating.keySet()) {
                intermediateResult.add(curEvaluating.get(orderSetKey));
            }

            List<List<SimplifiedTMSOrder>> bestIterationClusters = doSwappingIteration(intermediateResult, maxi, mini);
            int curCandidateSize = getNumSwapProposals(bestIterationClusters);
            if (curCandidateSize < bestSwappingProposals) {
                bestSwappingProposals = curCandidateSize;
                iteratedBest = bestIterationClusters;
                System.out.println(generateListSwapProposal(new ArrayList<>(curEvaluating.keySet()), curEvaluating).size());
            }
        }

        System.out.println("Total number of swap proposals existing in best solution: " + getNumSwapProposals(iteratedBest));
        saveClustersToFile(iteratedBest);
        return iteratedBest;
    }

    private List<SimplifiedTMSOrder> getAllCentroids(List<List<SimplifiedTMSOrder>> clusteredOrders) {
        Set<SimplifiedTMSOrder> centroids = new HashSet<>();
        for (List<SimplifiedTMSOrder> singleCluster : clusteredOrders) {
            centroids.add(electCentroid(singleCluster));
        }

        return new ArrayList<>(centroids);
    }

    private Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> reallocatePoints(final Map<SimplifiedTMSOrder, DistPair> closestDistMap, List<SimplifiedTMSOrder> orderedTMS,
                                                                               int mediumSize, List<SimplifiedTMSOrder> centroids) {
        System.out.println("The size of centroids is: " + centroids.size());
        // TODO this is the new model
        Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> clusterMap = new HashMap<>();
        for (SimplifiedTMSOrder centroid : centroids) {
            clusterMap.put(centroid, new ArrayList<SimplifiedTMSOrder>());
        }

        List<SimplifiedTMSOrder> centroidsCopy = new ArrayList<>();
        centroidsCopy.addAll(centroids);
        int initialMediumSize = mediumSize;
        int remainder = mediumSize * centroids.size() - orderedTMS.size();
        int threshold = centroids.size() - remainder;
        int numFullClusters = 0;
        while (!orderedTMS.isEmpty()) {
            // Sort based on the distance to its nearest cluster
            Collections.sort(orderedTMS, new Comparator<SimplifiedTMSOrder>() {

                @Override
                public int compare(SimplifiedTMSOrder o1, SimplifiedTMSOrder o2) {
                    DistPair o1s = closestDistMap.get(o1);
                    DistPair o2s = closestDistMap.get(o2);
                    if (o1s == o2s)
                        return 0;
                    if (o1s == null)
                        return -1;
                    if (o2s == null)
                        return 1;

                    if (o1s.distance > o2s.distance)
                        return -1;
                    if (o1s.distance < o2s.distance)
                        return 1;
                    return 0;
                }
            });

            Iterator<SimplifiedTMSOrder> orderIter = orderedTMS.iterator();
            while (orderIter.hasNext()) {
                SimplifiedTMSOrder currentOrder = orderIter.next();
                DistPair targetCluster = closestDistMap.get(currentOrder);
                if (clusterMap.get(targetCluster.order).size() >= mediumSize) {
                    if (centroidsCopy.contains(targetCluster.order)) {
                        numFullClusters++;
                        centroidsCopy.remove(targetCluster.order);
                    }
                    // Insert it back into the heap
                    closestDistMap.put(currentOrder, groupOnCentroid(centroidsCopy, currentOrder));
                    break;
                } else {
                    clusterMap.get(targetCluster.order).add(currentOrder);
                    orderIter.remove();
                    // If centroid has accumulated enough points, remove it from the copy
                    if (clusterMap.get(targetCluster.order).size() >= mediumSize) {
                        centroidsCopy.remove(targetCluster.order);
                        numFullClusters++;
                        if (numFullClusters >= threshold)
                            mediumSize = initialMediumSize - 1;
                    }
                }
            }
        }

        return clusterMap;
    }

    private List<List<SwapProposal>> generateListSwapProposal(List<SimplifiedTMSOrder> centroids,
                                                              Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> clusterMap) {
        List<List<SwapProposal>> swapProposals = new ArrayList<>();

        for (SimplifiedTMSOrder centroidKey : clusterMap.keySet()) {
            List<SimplifiedTMSOrder> clusteredFellows = clusterMap.get(centroidKey);
            for (SimplifiedTMSOrder singlePoint : clusteredFellows) {
                double originalDist = DropoffPointsDistanceMeasure.measurePoint(centroidKey.getLatlon(), singlePoint.getLatlon());
                List<SwapProposal> proposalsFromSinglePoint = new ArrayList<>();

                for (SimplifiedTMSOrder centroidToCompare : centroids) {
                    double newDistance = DropoffPointsDistanceMeasure.measurePoint(
                            centroidToCompare.getLatlon(), singlePoint.getLatlon());
                    if (newDistance < originalDist) {
                        SwapProposal swpp = new SwapProposal(singlePoint, centroidKey, centroidToCompare, originalDist - newDistance);
                        proposalsFromSinglePoint.add(swpp);
                    }
                }

                if (!proposalsFromSinglePoint.isEmpty()) {
                    Collections.sort(proposalsFromSinglePoint, new Comparator<SwapProposal>() {

                        @Override
                        public int compare(SwapProposal o1, SwapProposal o2) {
                            if (o1.improvement > o2.improvement)
                                return -1;
                            if (o1.improvement < o2.improvement)
                                return 1;
                            return 0;
                        }
                    });

                    swapProposals.add(proposalsFromSinglePoint);
                }
            }
        }

        return swapProposals;
    }

    private int getNumSwapProposals(List<List<SimplifiedTMSOrder>> totalClusters) {
        int swapProposalSum = 0;
        List<SimplifiedTMSOrder> allCentroids = getAllCentroids(totalClusters);
        for (List<SimplifiedTMSOrder> singleCluster : totalClusters) {
            SimplifiedTMSOrder centroid = electCentroid(singleCluster);
            for (SimplifiedTMSOrder individual : singleCluster)
                if (!centroid.equals(groupOnCentroid(allCentroids, individual).order)) {
                    swapProposalSum++;
                }
        }
        return swapProposalSum;
    }

    private void sortSwapProposalAlterativeList(List<List<SwapProposal>> swpps) {
        Collections.sort(swpps, (o1, o2) -> {
            if ((o1 == null || o1.isEmpty()) && (o2 == null || o2.isEmpty()))
                return 0;
            if (o1 == null || o1.isEmpty())
                return 1;
            if (o2 == null || o2.isEmpty())
                return -1;

            SwapProposal bestOf1 = o1.get(0);
            SwapProposal bestOf2 = o2.get(0);
            if (bestOf1.improvement > bestOf2.improvement)
                return -1;
            if (bestOf1.improvement < bestOf2.improvement)
                return 1;
            return 0;
        });
    }

    private static DistPair findMinDeficit_push(SimplifiedTMSOrder candidate, SimplifiedTMSOrder currentCenter,
                                                List<SimplifiedTMSOrder> centroids) {
        double minCost = Double.MAX_VALUE;
        SimplifiedTMSOrder minDeficit = null;
        for (SimplifiedTMSOrder c : centroids) {
            if (c.equals(currentCenter))
                continue;
            if (DropoffPointsDistanceMeasure.measurePoint(candidate.getLatlon(), c.getLatlon()) < minCost) {
                minCost = DropoffPointsDistanceMeasure.measurePoint(candidate.getLatlon(), c.getLatlon());
                minDeficit = c;
            }
        }

        return new DistPair(candidate, minDeficit, minCost);
    }

    private static DistPair findClusterMinDeficit_push(List<SimplifiedTMSOrder> clusterMembers,
                                                       SimplifiedTMSOrder currentCenter,
                                                       List<SimplifiedTMSOrder> centroids) {
        double minDeficitValue = Double.MAX_VALUE;
        DistPair bestLLR = null;
        for (SimplifiedTMSOrder singleMember : clusterMembers) {
            if (singleMember.equals(currentCenter))
                continue;
            DistPair evaluatedRelation = findMinDeficit_push(singleMember, currentCenter, centroids);
            double distToCompare = DropoffPointsDistanceMeasure.measurePoint(singleMember.getLatlon(),
                    currentCenter.getLatlon());
            if (evaluatedRelation.distance - distToCompare < minDeficitValue) {
                minDeficitValue = evaluatedRelation.distance - distToCompare;
                bestLLR = evaluatedRelation;
            }
        }

        return bestLLR;
    }

    private static SimplifiedTMSOrder[] findClusterMinDeficit_pull(SimplifiedTMSOrder targetCenter, Map<SimplifiedTMSOrder,
            List<SimplifiedTMSOrder>> clusteredMap, List<SimplifiedTMSOrder> allCentersLeft) {
        double minDeficit = Double.MAX_VALUE;
        SimplifiedTMSOrder minDeficitValue = null;
        SimplifiedTMSOrder minDeficitCenter = null;
        for (SimplifiedTMSOrder singleCentroid : allCentersLeft) {
            if (singleCentroid.equals(targetCenter))
                continue;
            DistPair bestInCluster = findMinDeficit_pull(clusteredMap.get(singleCentroid), targetCenter, singleCentroid);
            if (bestInCluster.distance < minDeficit) {
                minDeficit = bestInCluster.distance;
                minDeficitValue = bestInCluster.order;
                minDeficitCenter = singleCentroid;
            }
        }

        return new SimplifiedTMSOrder[]{minDeficitCenter, minDeficitValue};
    }

    private static DistPair findMinDeficit_pull(List<SimplifiedTMSOrder> clusterMembers, SimplifiedTMSOrder toCenter,
                                                SimplifiedTMSOrder originalCenter) {
        double minDeficit = Double.MAX_VALUE;
        SimplifiedTMSOrder minDeficitValue = null;
        for (SimplifiedTMSOrder singleMember : clusterMembers) {
            // the difference between distance to original center and new center should be minimized
            if (DropoffPointsDistanceMeasure.measurePoint(singleMember.getLatlon(), toCenter.getLatlon()) -
                    DropoffPointsDistanceMeasure.measurePoint(singleMember.getLatlon(), originalCenter.getLatlon()) < minDeficit) {
                minDeficit = DropoffPointsDistanceMeasure.measurePoint(singleMember.getLatlon(), toCenter.getLatlon()) -
                        DropoffPointsDistanceMeasure.measurePoint(singleMember.getLatlon(), originalCenter.getLatlon());
                minDeficitValue = singleMember;
            }
        }

        return new DistPair(minDeficitValue, toCenter, minDeficit);
    }

    private static boolean resolveHardSwapProposals_bothConstraints(Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> allClusters, SwapProposal sp) {
        // Use push model
        List<SimplifiedTMSOrder> allCentroids = new ArrayList<>(allClusters.keySet());
        // starting from the one the proposal is going to (since it's full)
        SimplifiedTMSOrder startCenter = sp.toCluster;
        // Where is the end cluster the chain is going?
        SimplifiedTMSOrder endCenter = sp.fromCluster;
        // The current cluster center we're considering
        SimplifiedTMSOrder currentCenter = startCenter;
        // never visit any cluster more than once
        allCentroids.remove(startCenter);
        Map<SimplifiedTMSOrder, Integer> sizeMap = new HashMap<>();
        for (SimplifiedTMSOrder cen : allClusters.keySet()) {
            sizeMap.put(cen, allClusters.get(cen).size());
        }

        // subtract one from the fromcluster since we'll first assume
        // the swap proposal is approved finally
        sizeMap.put(sp.fromCluster, sizeMap.get(sp.fromCluster) - 1);

        // The waypoints
        List<SimplifiedTMSOrder> chainedPath_centroid = new ArrayList<>();
        List<SimplifiedTMSOrder> chainedPath_element = new ArrayList<>();
        double accumulatedImprovement = sp.improvement;
        boolean stopCriteria = false;

        while (!stopCriteria && !allCentroids.isEmpty()) {
            DistPair currentBestRelation = findClusterMinDeficit_push(allClusters.get(currentCenter), currentCenter, allCentroids);
            double deficit = currentBestRelation.distance - DropoffPointsDistanceMeasure.measurePoint(
                    currentBestRelation.order.getLatlon(), currentCenter.getLatlon());
            accumulatedImprovement -= deficit;

            // there are waypoints if and only if the first cluster didn't pass our criteria
            allCentroids.remove(currentBestRelation.center);
            chainedPath_centroid.add(currentCenter);
            chainedPath_element.add(currentBestRelation.order);
            currentCenter = currentBestRelation.center;

            if (currentCenter.equals(endCenter)) {
                stopCriteria = true;
                break;
            }
        }

        if (accumulatedImprovement > 0 && stopCriteria) {
            SimplifiedTMSOrder lastSubject = sp.applicant;
            allClusters.get(sp.toCluster).add(lastSubject);
            allClusters.get(sp.fromCluster).remove(lastSubject);
            // approve the swap proposal and do the change
            for (int i = 0; i < chainedPath_centroid.size(); i++) {
                SimplifiedTMSOrder curCentroid = chainedPath_centroid.get(i);
                if (i != 0)
                    allClusters.get(curCentroid).add(lastSubject);
                lastSubject = chainedPath_element.get(i);
                boolean result = allClusters.get(curCentroid).remove(lastSubject);
            }

            allClusters.get(endCenter).add(lastSubject);
            System.out.println("Bothness -- Chained proposal finished!!!");
            return true;
        } else {
            // reject the swap proposal
            return false;
        }
    }

    private static boolean resolveHardSwapProposals_fromClusterEmpty(Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> allClusters, SwapProposal sp, int mini) {
        List<SimplifiedTMSOrder> allCentroids = new ArrayList<>(allClusters.keySet());
        // start borrowing points from fromCluster (since it has too few points)
        SimplifiedTMSOrder startCenter = sp.fromCluster;
        // Where is the end cluster the chain is going?
        SimplifiedTMSOrder endCenter = null;
        // The current cluster center we're considering
        SimplifiedTMSOrder currentCenter = startCenter;
        // never visit any cluster more than once
        allCentroids.remove(startCenter);
        Map<SimplifiedTMSOrder, Integer> sizeMap = new HashMap<>();
        for (SimplifiedTMSOrder cen : allClusters.keySet()) {
            sizeMap.put(cen, allClusters.get(cen).size());
        }

        // add one to toCluster since it's not over capacity
        sizeMap.put(sp.toCluster, sizeMap.get(sp.toCluster) - 1);

        // The waypoints
        List<SimplifiedTMSOrder> chainedPath_centroid = new ArrayList<>();
        List<SimplifiedTMSOrder> chainedPath_element = new ArrayList<>();
        double accumulatedImprovement = sp.improvement;
        boolean stopCriteria = false;

        while (!stopCriteria && !allCentroids.isEmpty()) {
            // find the best candidate and its center to pull
            SimplifiedTMSOrder[] currentBestRelation = findClusterMinDeficit_pull(currentCenter, allClusters, allCentroids);
            double deficit = DropoffPointsDistanceMeasure.measurePoint(currentBestRelation[1].getLatlon(), currentCenter.getLatlon()) -
                    DropoffPointsDistanceMeasure.measurePoint(currentBestRelation[0].getLatlon(), currentBestRelation[1].getLatlon());
            accumulatedImprovement -= deficit;

            // never consider the same cluster once again
            allCentroids.remove(currentBestRelation[0]);
            chainedPath_centroid.add(currentBestRelation[0]);
            chainedPath_element.add(currentBestRelation[1]);
            currentCenter = currentBestRelation[0];

            if (sizeMap.get(currentBestRelation[0]) > mini) {
                endCenter = currentBestRelation[0];
                stopCriteria = true;
                break;
            }
        }

        if (accumulatedImprovement > 0 && stopCriteria) {
            SimplifiedTMSOrder lastSubject = sp.applicant;
            allClusters.get(sp.toCluster).add(lastSubject);
            allClusters.get(sp.fromCluster).remove(lastSubject);
            allClusters.get(sp.fromCluster).add(chainedPath_element.get(0));
            allClusters.get(chainedPath_centroid.get(0)).remove(chainedPath_element.get(0));
            // approve the swap proposal and do the change
            for (int i = 0; i < chainedPath_centroid.size() - 1; i++) {
                SimplifiedTMSOrder curCentroid = chainedPath_centroid.get(i);
                allClusters.get(curCentroid).remove(chainedPath_element.get(i));
                allClusters.get(curCentroid).add(chainedPath_element.get(i + 1));
            }

            allClusters.get(endCenter).remove(chainedPath_element.get(chainedPath_element.size() - 1));
            System.out.println("Emptiness -- Chained proposal finished!!!");
            return true;
        } else {
            // reject the swap proposal
            return false;
        }
    }

    private static boolean resolveHardSwapProposals_toClusterFull(Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> allClusters, SwapProposal sp, int maxi) {
        List<SimplifiedTMSOrder> allCentroids = new ArrayList<>(allClusters.keySet());
        // starting from the one the proposal is going to (since it's full)
        SimplifiedTMSOrder startCenter = sp.toCluster;
        // Where is the end cluster the chain is going?
        SimplifiedTMSOrder endCenter = null;
        // The current cluster center we're considering
        SimplifiedTMSOrder currentCenter = startCenter;
        // never visit any cluster more than once
        allCentroids.remove(startCenter);
        Map<SimplifiedTMSOrder, Integer> sizeMap = new HashMap<>();
        for (SimplifiedTMSOrder cen : allClusters.keySet()) {
            sizeMap.put(cen, allClusters.get(cen).size());
        }

        // subtract one from the fromcluster since we'll first assume
        // the swap proposal is approved finally
        sizeMap.put(sp.fromCluster, sizeMap.get(sp.fromCluster) - 1);

        // The waypoints
        List<SimplifiedTMSOrder> chainedPath_centroid = new ArrayList<>();
        List<SimplifiedTMSOrder> chainedPath_element = new ArrayList<>();
        double accumulatedImprovement = sp.improvement;
        boolean stopCriteria = false;

        while (!stopCriteria && !allCentroids.isEmpty()) {
            DistPair currentBestRelation = findClusterMinDeficit_push(allClusters.get(currentCenter), currentCenter, allCentroids);

            double deficit = currentBestRelation.distance - DropoffPointsDistanceMeasure.measurePoint(
                    currentBestRelation.order.getLatlon(), currentCenter.getLatlon());
            accumulatedImprovement -= deficit;

            // there are waypoints if and only if the first cluster didn't pass our criteria
            allCentroids.remove(currentBestRelation.center);
            chainedPath_centroid.add(currentCenter);
            chainedPath_element.add(currentBestRelation.order);
            currentCenter = currentBestRelation.center;

            if (sizeMap.get(currentBestRelation.center) < maxi) {
                endCenter = currentBestRelation.center;
                stopCriteria = true;
                break;
            }
        }

        if (accumulatedImprovement > 0 && stopCriteria) {
            SimplifiedTMSOrder lastSubject = sp.applicant;
            allClusters.get(sp.toCluster).add(lastSubject);
            allClusters.get(sp.fromCluster).remove(lastSubject);
            // approve the swap proposal and do the change
            for (int i = 0; i < chainedPath_centroid.size(); i++) {
                SimplifiedTMSOrder curCentroid = chainedPath_centroid.get(i);
                if (i != 0)
                    allClusters.get(curCentroid).add(lastSubject);
                lastSubject = chainedPath_element.get(i);
                allClusters.get(curCentroid).remove(lastSubject);
            }

            allClusters.get(endCenter).add(lastSubject);
            System.out.println("Chained proposal finished!!!");
            return true;
        } else {
            // reject the swap proposal
            return false;
        }
    }

    private List<List<SimplifiedTMSOrder>> doSwappingIteration(List<List<SimplifiedTMSOrder>> orderCentroidMapping,
                                                               int maxSize, int minSize) {
        for (int i = 0; i < 100; i++) {
            Map<SimplifiedTMSOrder, List<SimplifiedTMSOrder>> clusteredMap = new HashMap<>();
            for (List<SimplifiedTMSOrder> singleCluster : orderCentroidMapping) {
                clusteredMap.put(electCentroid(singleCluster), singleCluster);
            }
            List<SimplifiedTMSOrder> mappingCentroids = new ArrayList<>(clusteredMap.keySet());
            List<List<SwapProposal>> swpps = generateListSwapProposal(mappingCentroids, clusteredMap);
            System.out.println("Size of swap proposals is: " + swpps.size());

            // New solution:
            // Effects: May increase the number of improper clustered points,
            // but for every single one, it is not very far from its nearest cluster.
            outerloop_endIteration:
            while (swpps.size() > 0) {
                sortSwapProposalAlterativeList(swpps);
                Iterator<List<SwapProposal>> slistIter = swpps.iterator();
                while (slistIter.hasNext()) {
                    List<SwapProposal> alternatives = slistIter.next();
                    if (alternatives.isEmpty()) {
                        slistIter.remove();
                        continue;
                    }
                    SwapProposal bestSwap = alternatives.get(0);
                    SimplifiedTMSOrder subjectToSwap = bestSwap.applicant;
                    SimplifiedTMSOrder desiredClusterCenter = bestSwap.toCluster;
                    SimplifiedTMSOrder hatredClusterCenter = bestSwap.fromCluster;
                    if (clusteredMap.get(desiredClusterCenter).size() < maxSize && clusteredMap.get(hatredClusterCenter).size() > minSize) {
                        clusteredMap.get(desiredClusterCenter).add(subjectToSwap);
                        clusteredMap.get(hatredClusterCenter).remove(subjectToSwap);
                        slistIter.remove();
                    } else {
                        List<SwapProposal> bestProposalFromDesiredCluster = null;
                        double bestImprovement = Double.MIN_VALUE;

                        // TODO don't know if this will throw comodification exception
                        for (List<SwapProposal> allSwapProposalsRemaining : swpps) {
                            if (allSwapProposalsRemaining.isEmpty())
                                continue;
                            SwapProposal representative = allSwapProposalsRemaining.get(0);
                            if (representative.fromCluster.equals(desiredClusterCenter) && representative.improvement > bestImprovement) {
                                bestProposalFromDesiredCluster = allSwapProposalsRemaining;
                                bestImprovement = representative.improvement;
                            }
                        }

                        if (bestProposalFromDesiredCluster != null) {
                            SimplifiedTMSOrder anotherSubject = bestProposalFromDesiredCluster.get(0).applicant;
                            clusteredMap.get(desiredClusterCenter).add(subjectToSwap);
                            clusteredMap.get(hatredClusterCenter).remove(subjectToSwap);
                            clusteredMap.get(desiredClusterCenter).remove(anotherSubject);
                            clusteredMap.get(hatredClusterCenter).add(anotherSubject);
                            bestProposalFromDesiredCluster.clear();
                            slistIter.remove();
                        } else {
                            boolean result = false;
                            if (!(clusteredMap.get(desiredClusterCenter).size() < maxSize ||
                                    clusteredMap.get(hatredClusterCenter).size() > minSize)) {
                                result = resolveHardSwapProposals_bothConstraints(clusteredMap, alternatives.get(0));
                            } else if (clusteredMap.get(desiredClusterCenter).size() >= maxSize) {
                                result = resolveHardSwapProposals_toClusterFull(clusteredMap, alternatives.get(0), maxSize);
                            } else {
                                result = resolveHardSwapProposals_fromClusterEmpty(clusteredMap, alternatives.get(0), minSize);
                            }

                            if (result) {
                                break outerloop_endIteration;
                            } else {
                                alternatives.remove(0);
                                break;
                            }
//                            boolean result = resolveHardSwapProposals_toClusterFull(clusteredMap, alternatives.get(0), maxSize);
//                            if (result) {
//                                break outerloop_endIteration;
//                            } else {
//                                alternatives.remove(0);
//                                break;
//                            }
                        }
                    }
                }
            }
        }
        return orderCentroidMapping;
    }

    private DistPair groupOnCentroid(List<SimplifiedTMSOrder> centroids, SimplifiedTMSOrder toGroup) {
        double minDistance = 1000000000000L;
        SimplifiedTMSOrder closest = null;
        for (SimplifiedTMSOrder clusterCentroid : centroids) {
            double actualDistance = DropoffPointsDistanceMeasure.measurePoint(toGroup.getLatlon(), clusterCentroid.getLatlon());
            if (actualDistance < minDistance) {
                minDistance = actualDistance;
                closest = clusterCentroid;
            }
        }
        return new DistPair(closest, minDistance);
    }

    public static List<BasePoint> populateBasePoints() {
        List<BasePoint> returnVal = new ArrayList<>();
        String bpURLStr = "http://localhost:8080/tms/routePlanningController.do?allBP";
        URL bpURL = null;
        try {
            bpURL = new URL(bpURLStr);
            // TODO: get all base points using api
            BufferedReader br = new BufferedReader(new InputStreamReader(bpURL.openStream()));
            String line = "", totalLine = "";
            while ((line = br.readLine()) != null) {
                totalLine += line;
                System.out.println(line);
            }

            JSONObject jo = new JSONObject(totalLine);
            JSONArray array = jo.getJSONArray("obj");
            for (Object singlePoint : array) {
                returnVal.add(new BasePoint(((JSONObject) singlePoint).getString("baidu_latitude"),
                        ((JSONObject) singlePoint).getString("baidu_longitude"),
                        ((JSONObject) singlePoint).getString("osm_latitude"),
                        ((JSONObject) singlePoint).getString("osm_longitude")));
            }
        } catch (IOException e) {
            return null;
        }

        return returnVal;
    }

    public void relateWithBasePoints(List<SimplifiedTMSOrder> orders) {
//        populateBasePoints();
        Map<BasePoint, List<SimplifiedTMSOrder>> retVal = new HashMap<BasePoint, List<SimplifiedTMSOrder>>();
        if (allBasePoints.isEmpty())
            bpMap = retVal;

        for (SimplifiedTMSOrder ord : orders) {
            BasePoint minDistance = allBasePoints.get(0);
            BasePoint minDistance2 = allBasePoints.get(1);
            BasePoint minDistance3 = allBasePoints.get(2);
            double minDist = GeoPolygon.getEarthDistance("" + ord.getLatlon().getLat(), "" + ord.getLatlon().getLon(),
                    minDistance.getBaidu_latitude(), minDistance.getBaidu_longitude());
            double minDist2 = GeoPolygon.getEarthDistance("" + ord.getLatlon().getLat(), "" + ord.getLatlon().getLon(),
                    minDistance2.getBaidu_latitude(), minDistance2.getBaidu_longitude());
            double minDist3 = GeoPolygon.getEarthDistance("" + ord.getLatlon().getLat(), "" + ord.getLatlon().getLon(),
                    minDistance3.getBaidu_latitude(), minDistance3.getBaidu_longitude());

            if (minDist3 < minDist2) {
                BasePoint temp = minDistance2;
                minDistance2 = minDistance3;
                minDistance3 = temp;
            }

            if (minDist2 < minDist) {
                BasePoint temp = minDistance2;
                minDistance2 = minDistance;
                minDistance3 = temp;
            }

            if (minDist3 < minDist2) {
                BasePoint temp = minDistance2;
                minDistance2 = minDistance3;
                minDistance3 = temp;
            }

            minDist = GeoPolygon.getEarthDistance("" + ord.getLatlon().getLat(), "" + ord.getLatlon().getLon(),
                    minDistance.getBaidu_latitude(), minDistance.getBaidu_longitude());
            minDist2 = GeoPolygon.getEarthDistance("" + ord.getLatlon().getLat(), "" + ord.getLatlon().getLon(),
                    minDistance2.getBaidu_latitude(), minDistance2.getBaidu_longitude());
            minDist3 = GeoPolygon.getEarthDistance("" + ord.getLatlon().getLat(), "" + ord.getLatlon().getLon(),
                    minDistance3.getBaidu_latitude(), minDistance3.getBaidu_longitude());
            for (BasePoint bp : allBasePoints) {
                double curDistance = GeoPolygon.getEarthDistance("" + ord.getLatlon().getLat(), "" + ord.getLatlon().getLon(),
                        bp.getBaidu_latitude(), bp.getBaidu_longitude());
                if (curDistance < minDist2) {
                    if (curDistance < minDist) {
                        minDist3 = minDist2;
                        minDist2 = minDist;
                        minDist = curDistance;
                        minDistance3 = minDistance2;
                        minDistance2 = minDistance;
                        minDistance = bp;
                    } else {
                        minDist3 = minDist2;
                        minDist2 = curDistance;
                        minDistance3 = minDistance2;
                        minDistance2 = bp;
                    }
                } else if (curDistance < minDist3) {
                    minDist3 = curDistance;
                    minDistance3 = bp;
                }
            }

            if (retVal.containsKey(minDistance))
                retVal.get(minDistance).add(ord);
            else
                retVal.put(minDistance, new ArrayList<SimplifiedTMSOrder>(Arrays.asList(new SimplifiedTMSOrder[]{ord})));
        }
        bpMap = retVal;
    }

    private TMSRoutePlan doGA(List<SimplifiedTMSOrder> allDropoffPoints, final int minTour, final int maxTour, boolean heaviness,
                              boolean considerReturning) throws InterruptedException {
        // Set initial capacity to population size in order to do less resize
        final List<TMSRoutePlan> population = new ArrayList<>(populationSize);
        final List<TMSRoutePlan> tempPopulation = new ArrayList<>(populationSize * methodCount);
        long globalMin = Long.MAX_VALUE;
        TMSRoutePlan optimalPlan = null;
        int numDestinations = allDropoffPoints.size();

        // First, initialise the population.
        for (int i = 0; i < populationSize; i++) {
            TMSRoutePlan candidate;
            if (bundled.isEmpty())
                candidate = new TMSRoutePlan(allDropoffPoints, new ArrayList<>(), minTour, maxTour);
            else
                candidate = new TMSRoutePlan(allDropoffPoints, new ArrayList<>(), bundled, minTour, maxTour);
            if (candidate.hasBundles())
                candidate.randomBreaksWithBundle();
            else
                candidate.randomBreaks();
            population.add(candidate);
        }

//		if (true)
//			return population.get(0);

        // Handles the case where population size is 0 and 1.
        if (allDropoffPoints.size() < 2)
            return population.isEmpty() ? null : population.get(0);

        int loopIterationCount = 1000;
        if (!heaviness)
            loopIterationCount = allDropoffPoints.size() * allDropoffPoints.size();

        // TODO change this
        for (int i = 0; i < loopIterationCount; i++) {
            long iterationMin = Long.MAX_VALUE;
            tempPopulation.clear();
            int firstInsertionPointInit = new Random().nextInt(numDestinations);
            int secondInsertionPointInit = new Random().nextInt(numDestinations);

            while (firstInsertionPointInit == secondInsertionPointInit) {
                secondInsertionPointInit = new Random().nextInt(numDestinations);
            }

            // Swap values if reverse
            if (firstInsertionPointInit > secondInsertionPointInit) {
                firstInsertionPointInit = firstInsertionPointInit ^ secondInsertionPointInit;
                secondInsertionPointInit = firstInsertionPointInit ^ secondInsertionPointInit;
                firstInsertionPointInit = firstInsertionPointInit ^ secondInsertionPointInit;
            }

            final int firstInsertionPoint = firstInsertionPointInit;
            final int secondInsertionPoint = secondInsertionPointInit;

            // Whether or not to add more methods for alteration

            Thread t1 = new Thread(() -> {
                for (int popIndex = 0; popIndex < populationSize; popIndex++) {
                    TMSRoutePlan curRP = population.get(popIndex);
                    final List<SimplifiedTMSOrder> curPopSP = curRP.getPoints();
                    final List<Integer> curBreak = curRP.getBreaks();
                    tempPopulation.add(curRP);
                    // Flip
                    ArrayList<SimplifiedTMSOrder> tempSPForFlipping = new ArrayList<SimplifiedTMSOrder>();
                    ArrayList<Integer> tempBreaksForFlipping = new ArrayList<Integer>();
                    tempSPForFlipping.addAll(curPopSP);
                    tempBreaksForFlipping.addAll(curBreak);
                    for (int index = firstInsertionPoint; index <= secondInsertionPoint; index++) {
                        tempSPForFlipping.set(firstInsertionPoint +
                                secondInsertionPoint - index, curPopSP.get(index));
                    }
                    if (bundled.isEmpty())
                        tempPopulation.add(new TMSRoutePlan(tempSPForFlipping, tempBreaksForFlipping, minTour, maxTour));
                    else
                        tempPopulation.add(new TMSRoutePlan(tempSPForFlipping, tempBreaksForFlipping, bundled, minTour, maxTour));
                }
            });

            Thread t2 = new Thread(() -> {
                for (int popIndex = 0; popIndex < populationSize; popIndex++) {
                    TMSRoutePlan curRP = population.get(popIndex);
                    final List<SimplifiedTMSOrder> curPopSP = curRP.getPoints();
                    final List<Integer> curBreak = curRP.getBreaks();
                    tempPopulation.add(curRP);
                    // Swap
                    ArrayList<SimplifiedTMSOrder> tempSPForSwapping = new ArrayList<SimplifiedTMSOrder>();
                    ArrayList<Integer> tempBreaksForSwapping = new ArrayList<Integer>();
                    tempSPForSwapping.addAll(curPopSP);
                    tempBreaksForSwapping.addAll(curBreak);
                    tempSPForSwapping.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
                    tempSPForSwapping.set(secondInsertionPoint, curPopSP.get(firstInsertionPoint));
                    if (bundled.isEmpty())
                        tempPopulation.add(new TMSRoutePlan(tempSPForSwapping, tempBreaksForSwapping, minTour, maxTour));
                    else
                        tempPopulation.add(new TMSRoutePlan(tempSPForSwapping, tempBreaksForSwapping, bundled, minTour, maxTour));
                }
            });

            Thread t3 = new Thread(() -> {
                for (int popIndex = 0; popIndex < populationSize; popIndex++) {
                    TMSRoutePlan curRP = population.get(popIndex);
                    final List<SimplifiedTMSOrder> curPopSP = curRP.getPoints();
                    final List<Integer> curBreak = curRP.getBreaks();
                    tempPopulation.add(curRP);
                    // Slide
                    ArrayList<SimplifiedTMSOrder> tempSPForSliding = new ArrayList<>();
                    ArrayList<Integer> tempBreaksForSliding = new ArrayList<>();
                    tempSPForSliding.addAll(curPopSP);
                    tempBreaksForSliding.addAll(curBreak);
                    for (int index = firstInsertionPoint + 1; index <= secondInsertionPoint; index++) {
                        tempSPForSliding.set(index, curPopSP.get(index - 1));
                    }
                    tempSPForSliding.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
                    if (bundled.isEmpty())
                        tempPopulation.add(new TMSRoutePlan(tempSPForSliding, tempBreaksForSliding, minTour, maxTour));
                    else
                        tempPopulation.add(new TMSRoutePlan(tempSPForSliding, tempBreaksForSliding, bundled, minTour, maxTour));
                }
            });

            Thread t4 = new Thread(() -> {
                for (int popIndex = 0; popIndex < populationSize; popIndex++) {
                    TMSRoutePlan curRP = population.get(popIndex);
                    final List<SimplifiedTMSOrder> curPopSP = curRP.getPoints();
                    tempPopulation.add(curRP);
                    // Modify breaks
                    ArrayList<SimplifiedTMSOrder> tempSPForMB = new ArrayList<SimplifiedTMSOrder>();
                    tempSPForMB.addAll(curPopSP);
                    if (bundled.isEmpty()) {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForMB, new ArrayList<>(), minTour, maxTour);
                        MBCandidate.randomBreaks();
                        tempPopulation.add(MBCandidate);
                    } else {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForMB, new ArrayList<>(), bundled, minTour, maxTour);
                        MBCandidate.randomBreaksWithBundle();
                        tempPopulation.add(MBCandidate);
                    }
                }
            });

            Thread t5 = new Thread(() -> {
                for (int popIndex = 0; popIndex < populationSize; popIndex++) {
                    TMSRoutePlan curRP = population.get(popIndex);
                    final List<SimplifiedTMSOrder> curPopSP = curRP.getPoints();
                    final List<Integer> curBreak = curRP.getBreaks();
                    tempPopulation.add(curRP);
                    // Flip & Modify breaks
                    ArrayList<SimplifiedTMSOrder> tempSPForFlipping = new ArrayList<SimplifiedTMSOrder>();
                    ArrayList<Integer> tempBreaksForFlipping = new ArrayList<>();
                    tempSPForFlipping.addAll(curPopSP);
                    tempBreaksForFlipping.addAll(curBreak);
                    for (int index = firstInsertionPoint; index <= secondInsertionPoint; index++) {
                        tempSPForFlipping.set(firstInsertionPoint +
                                secondInsertionPoint - index, curPopSP.get(index));
                    }
                    if (bundled.isEmpty()) {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForFlipping, new ArrayList<Integer>(), minTour, maxTour);
                        MBCandidate.randomBreaks();
                        tempPopulation.add(MBCandidate);
                    } else {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForFlipping, new ArrayList<Integer>(), bundled, minTour, maxTour);
                        MBCandidate.randomBreaksWithBundle();
                        tempPopulation.add(MBCandidate);
                    }
                }
            });

            Thread t6 = new Thread(() -> {
                for (int popIndex = 0; popIndex < populationSize; popIndex++) {
                    TMSRoutePlan curRP = population.get(popIndex);
                    final List<SimplifiedTMSOrder> curPopSP = curRP.getPoints();
                    final List<Integer> curBreak = curRP.getBreaks();
                    tempPopulation.add(curRP);
                    // Swap & Modify breaks
                    ArrayList<SimplifiedTMSOrder> tempSPForSwapping = new ArrayList<>();
                    ArrayList<Integer> tempBreaksForSwapping = new ArrayList<>();
                    tempSPForSwapping.addAll(curPopSP);
                    tempBreaksForSwapping.addAll(curBreak);
                    tempSPForSwapping.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
                    tempSPForSwapping.set(secondInsertionPoint, curPopSP.get(firstInsertionPoint));
                    if (bundled.isEmpty()) {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForSwapping, new ArrayList<>(), minTour, maxTour);
                        MBCandidate.randomBreaks();
                        tempPopulation.add(MBCandidate);
                    } else {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForSwapping, new ArrayList<>(), bundled, minTour, maxTour);
                        MBCandidate.randomBreaksWithBundle();
                        tempPopulation.add(MBCandidate);
                    }
                }
            });

            Thread t7 = new Thread(() -> {
                for (int popIndex = 0; popIndex < populationSize; popIndex++) {
                    TMSRoutePlan curRP = population.get(popIndex);
                    final List<SimplifiedTMSOrder> curPopSP = curRP.getPoints();
                    final List<Integer> curBreak = curRP.getBreaks();
                    tempPopulation.add(curRP);
                    // Slide & Modify breaks
                    ArrayList<SimplifiedTMSOrder> tempSPForSliding = new ArrayList<>();
                    ArrayList<Integer> tempBreaksForSliding = new ArrayList<>();
                    tempSPForSliding.addAll(curPopSP);
                    tempBreaksForSliding.addAll(curBreak);
                    for (int index = firstInsertionPoint + 1; index <= secondInsertionPoint; index++) {
                        tempSPForSliding.set(index, curPopSP.get(index - 1));
                    }
                    tempSPForSliding.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
                    if (bundled.isEmpty()) {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForSliding, new ArrayList<>(), minTour, maxTour);
                        MBCandidate.randomBreaks();
                        tempPopulation.add(MBCandidate);
                    } else {
                        TMSRoutePlan MBCandidate = new TMSRoutePlan(tempSPForSliding, new ArrayList<>(), bundled, minTour, maxTour);
                        MBCandidate.randomBreaksWithBundle();
                        tempPopulation.add(MBCandidate);
                    }
                }
            });

            t1.start();
            t1.join();
            t2.start();
            t2.join();
            t3.start();
            t3.join();
            t4.start();
            t4.join();
            t5.start();
            t5.join();
            t6.start();
            t6.join();
            t7.start();
            t7.join();

            Collections.shuffle(tempPopulation);
            population.clear();
            for (int split = 0; split < populationSize; split++) {
                long curMinCost = Long.MAX_VALUE;
                int curMinCostIndex = -1;

                // Whether or not to choose the max (MAY CHOOSE MAX WITH SOME PROBABILITY)
                for (int subIndex = 0; subIndex < methodCount; subIndex++) {
                    TMSRoutePlan curTMSPlanToEvaluate = tempPopulation.get(split * methodCount + subIndex);
                    int curCost = 0;
                    if (TMSRoutePlan.depotLatitude.equals(TMSRoutePlan.depotLatitudePerm) &&
                            TMSRoutePlan.depotLongitude.equals(TMSRoutePlan.depotLongitudePerm)) {
                        curCost = curTMSPlanToEvaluate.getRealTotalCost();
                    } else {
                        curCost = curTMSPlanToEvaluate.getTotalCostWithCustDepot();
                    }
                    if (curCost < curMinCost) {
                        curMinCost = curCost;
                        curMinCostIndex = subIndex;
                        optimalPlan = curTMSPlanToEvaluate;
                    }
                }

                if (curMinCost < globalMin) {
                    globalMin = curMinCost;
                }

                if (curMinCost < iterationMin) {
                    iterationMin = curMinCost;
                }
                population.add(tempPopulation.get(split * methodCount + curMinCostIndex));
            }

//			System.out.println("Global min is: " + globalMin);
        }

        System.out.println("The real optimalPlan cost is " + optimalPlan.getRealTotalCost());
        System.out.println("And the optimalPlan cost plus offset is: " + optimalPlan.getTotalCost());
        return optimalPlan;
    }

//    public GeoFence getGeofenceByID(int geofenceID) {
//        return geoFenceDao.getFenceByID(geofenceID);
//    }

    public static int getFenceID(LatLon ll) {
        for (GeoPolygon gp : fences) {
            if (gp.isPointInPolygon(ll))
                return gp.getOrder();
        }

        return 0;
    }

    private static class DistPair {
        SimplifiedTMSOrder order;
        SimplifiedTMSOrder center;
        double distance;

        private DistPair(SimplifiedTMSOrder order, double distance) {
            this.order = order;
            this.distance = distance;
        }

        private DistPair(SimplifiedTMSOrder order, SimplifiedTMSOrder center, double distance) {
            this.order = order;
            this.center = center;
            this.distance = distance;
        }
    }

    private static class SwapProposal {
        SimplifiedTMSOrder applicant;
        SimplifiedTMSOrder fromCluster;
        SimplifiedTMSOrder toCluster;
        double improvement;

        private SwapProposal(SimplifiedTMSOrder applicant, SimplifiedTMSOrder from, SimplifiedTMSOrder to, double impr) {
            this.applicant = applicant;
            fromCluster = from;
            toCluster = to;
            improvement = impr;
        }
    }
}
