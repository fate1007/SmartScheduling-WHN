package Util;

import Algorithm.RoutePlanningService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TMSRoutePlan {
    private List<SimplifiedTMSOrder> points = new ArrayList<SimplifiedTMSOrder>();
    private List<Integer> breaks = new ArrayList<Integer>();
    private List<List<SimplifiedTMSOrder>> bundles = new ArrayList<List<SimplifiedTMSOrder>>();
    private static int crossFenceCost = 7;
    private static int intoFenceCost = 12;
    public static final String depotAddr = "上海市普陀区柳园路599号";
    public static final String depotLatitudePerm = "31.301759";
    public static final String depotLongitudePerm = "121.348674";
    public static SimplifiedTMSOrder depotLocationPerm = new SimplifiedTMSOrder(new TMSOrderLabel("19940814", depotAddr),
            new LatLon(Double.parseDouble(depotLatitudePerm), Double.parseDouble(depotLongitudePerm)));

    // 121.220891,31.358725
    public static String depotLatitude = "31.358725";
    public static String depotLongitude = "121.220891";
    public static SimplifiedTMSOrder depotLocation = new SimplifiedTMSOrder(new TMSOrderLabel("19940814", depotAddr),
            new LatLon(Double.parseDouble(depotLatitude), Double.parseDouble(depotLongitude)));
    private int minTour = 3;
    private int maxTour = 7;
    private int numCars;

    public TMSRoutePlan(List<SimplifiedTMSOrder> dropoffPts, List<Integer> breaks,
                        List<List<SimplifiedTMSOrder>> bundles, int minTour, int maxTour) {
        this.setPoints(dropoffPts);
        this.setBreaks(breaks);
        this.bundles = bundles;

        if (minTour >= 1)
            this.minTour = minTour;
        if (maxTour >= 3)
            this.maxTour = maxTour;

        int minCars = (int) Math.ceil((double) (points.size() - getExtraPointsTaken(bundles)) / (double) maxTour) + bundles.size();
        int maxCars = (int) Math.floor((double) (points.size() - getExtraPointsTaken(bundles)) / (double) minTour) + bundles.size();
        if (minCars == maxCars)
            numCars = maxCars;
        else
            numCars = minCars + new Random().nextInt(maxCars - minCars + 1);
    }

    public static void restoreDepot() {
        depotLongitude = depotLongitudePerm;
        depotLatitude = depotLatitudePerm;
        depotLocation = depotLocationPerm;
    }

    public static void configureDepot(SimplifiedTMSOrder customDepot) {
        if (customDepot == null)
            return;
        depotLatitude = String.valueOf(customDepot.getLatlon().getLat());
        depotLongitude = String.valueOf(customDepot.getLatlon().getLon());
        depotLocation = customDepot;
    }

    public TMSRoutePlan(List<SimplifiedTMSOrder> points, List<Integer> breaks, int minTour, int maxTour) {
        this.setPoints(points);
        this.setBreaks(breaks);

        if (minTour >= 1)
            this.minTour = minTour;
        if (maxTour >= 3)
            this.maxTour = maxTour;

        int minCars = (int) Math.ceil((double) points.size() / (double) maxTour);
        int maxCars = (int) Math.floor((double) points.size() / (double) minTour);
        if (minCars == maxCars)
            numCars = maxCars;
        else
            numCars = minCars + new Random().nextInt(maxCars - minCars + 1);
    }

    public TMSRoutePlan(List<SimplifiedTMSOrder> points, List<Integer> breaks) {
        this.points = points;
        this.breaks = breaks;
    }

    private int getExtraPointsTaken(List<List<SimplifiedTMSOrder>> bundledOrders) {
        int extraSum = 0;
        for (List<SimplifiedTMSOrder> singleBundle : bundledOrders) {
            extraSum += (minTour - singleBundle.size() > 0) ? minTour - singleBundle.size() : 0;
        }

        return extraSum;
    }

    private int getTotalCostWithStartDest(SimplifiedTMSOrder origin, SimplifiedTMSOrder destination, boolean plusOffset) {
        Collections.sort(getBreaks());
        int sum = 0;
        int lastBreak = 0;
        int lastFenceID = 0;
        int fenceOffset = 0;
        for (int i = 0; i < getBreaks().size(); i++) {
            sum += DropoffPointsDistanceMeasure.measurePoint(origin, getPoints().get(lastBreak));
            sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(breaks.get(i) - 1), destination);
            lastFenceID = RoutePlanningService.getFenceID(points.get(lastBreak).getLatlon());
            for (int j = lastBreak + 1; j < getBreaks().get(i); j++) {
                sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(j), getPoints().get(j - 1));
                int currentFenceID = RoutePlanningService.getFenceID(getPoints().get(j).getLatlon());
                if (currentFenceID != lastFenceID) {
                    if (lastFenceID == 0 || currentFenceID == 0) {
                        fenceOffset += intoFenceCost;
                    } else if (Math.abs(lastFenceID - currentFenceID) == 2) {
                        fenceOffset += 2 * crossFenceCost;
                    } else {
                        fenceOffset += crossFenceCost;
                    }
                }
            }
            lastBreak = getBreaks().get(i);
        }

        sum += DropoffPointsDistanceMeasure.measurePoint(origin, getPoints().get(lastBreak));
        sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(getPoints().size() - 1), destination);
        for (int i = lastBreak + 1; i < getPoints().size(); i++) {
            sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(i), getPoints().get(i - 1));
        }

        if (plusOffset)
            return sum + fenceOffset;
        else
            return sum;
    }

    private int getTotalCost(boolean plusOffset) {
        return getTotalCostWithStartDest(depotLocation, depotLocation, plusOffset);
    }

    public int getTotalCostWithCustDepot(SimplifiedTMSOrder customerDepot) {
        Collections.sort(getBreaks());
        int sum = 0;
        int lastBreak = 0;
        for (int i = 0; i < getBreaks().size(); i++) {
            sum += DropoffPointsDistanceMeasure.measurePoint(depotLocation, customerDepot);
            sum += DropoffPointsDistanceMeasure.measurePoint(customerDepot, getPoints().get(lastBreak));
            sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(breaks.get(i) - 1), depotLocation);
            for (int j = lastBreak + 1; j < getBreaks().get(i); j++) {
                sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(j), getPoints().get(j - 1));
            }
            lastBreak = getBreaks().get(i);
        }

        sum += DropoffPointsDistanceMeasure.measurePoint(depotLocation, customerDepot);
        sum += DropoffPointsDistanceMeasure.measurePoint(customerDepot, getPoints().get(lastBreak));
        sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(getPoints().size() - 1), depotLocation);
        for (int i = lastBreak + 1; i < getPoints().size(); i++) {
            sum += DropoffPointsDistanceMeasure.measurePoint(getPoints().get(i), getPoints().get(i - 1));
        }
        return sum;
    }

    public int getTotalCost() {
        return getTotalCost(true);
    }

    public int getRealTotalCost() {
        return getTotalCost(false);
    }

    public void randomBreaks() {
        getBreaks().clear();

        if (numCars <= 1)
            return;

        // no constraints at all
        if (minTour < 2 && maxTour < 0) {
            while (getBreaks().size() < numCars - 1) {
                int randBreak = 1 + new Random().nextInt(points.size() - 1);
                if (!getBreaks().contains(randBreak)) {
                    getBreaks().add(randBreak);
                }
            }
        } else if (minTour > 1 && maxTour < 0) {
            // Only minTour needs to be considered
            for (int i = 0; i < numCars - 1; i++) {
                getBreaks().add(minTour);
            }

            int degreeOfFreedom = points.size() - numCars * minTour;
            while (degreeOfFreedom > 0) {
                int randomIndex = new Random().nextInt(numCars);
                if (randomIndex < breaks.size())
                    getBreaks().set(randomIndex, getBreaks().get(randomIndex) + 1);
                degreeOfFreedom--;
            }

            getBreaks().set(0, getBreaks().get(0) - 1);
            int sum = getBreaks().get(0);
            for (int i = 1; i < getBreaks().size(); i++) {
                sum += getBreaks().get(i);
                getBreaks().set(i, sum);
            }
        } else {
            // Both minTour and maxTour need to be considered
            for (int i = 0; i < numCars - 1; i++) {
                getBreaks().add(minTour);
            }

            int lastCar = minTour;
            int degreeOfFreedom = points.size() - numCars * minTour;
            while (degreeOfFreedom > 0) {
                int randomIndex = 0;
                do {
                    randomIndex = new Random().nextInt(numCars);
                } while ((randomIndex >= breaks.size() && lastCar >= maxTour) ||
                        ((randomIndex < breaks.size()) && getBreaks().get(randomIndex) >= maxTour));
                if (randomIndex < breaks.size())
                    getBreaks().set(randomIndex, getBreaks().get(randomIndex) + 1);
                else
                    lastCar ++;
                degreeOfFreedom--;
            }

            getBreaks().set(0, getBreaks().get(0) - 1);
            int sum = getBreaks().get(0);
            for (int i = 1; i < getBreaks().size(); i++) {
                sum += getBreaks().get(i);
                getBreaks().set(i, sum);
            }
        }
    }

    public boolean hasBundles() {
        return !bundles.isEmpty();
    }

    public void randomBreaksWithBundle() {
        if (bundles.size() > numCars)
            throw new IllegalArgumentException("Cannot finish load balancing under current context since"
                    + " there're too many bundles! Bundles size: " + bundles.size() + ", NumCars: " + numCars);
        if (minTour < 2 && maxTour < 0) {
            // No constraints to consider
            while (getBreaks().size() < numCars - 1) {
                int randBreak = 1 + new Random().nextInt(points.size() - 1);
                if (!getBreaks().contains(randBreak)) {
                    getBreaks().add(randBreak);
                }
            }
        } else if (minTour > 1 && maxTour < 0) {
            // Only minTour needs to be considered.
            int alreadyAdded = 0;

            // Do initial loading for break values.
            for (int i = 0; i < numCars - Math.max(1, bundles.size()); i++) {
                breaks.add(minTour);
                alreadyAdded += minTour;
            }

            for (int i = numCars - Math.max(1, bundles.size()); i < numCars - 1; i++) {
                if (bundles.get(i - (numCars - bundles.size() - 1)).size() >= minTour) {
                    alreadyAdded += bundles.get(i - (numCars - bundles.size() - 1)).size();
                    breaks.add(0);
                } else {
                    alreadyAdded += minTour;
                    breaks.add(minTour - bundles.get(i - (numCars - bundles.size() - 1)).size());
                }
            }

            if (bundles.get(bundles.size() - 1).size() >= minTour) {
                alreadyAdded += bundles.get(bundles.size() - 1).size();
                breaks.add(0);
            } else {
                alreadyAdded += minTour;
                breaks.add(minTour - bundles.get(bundles.size() - 1).size());
            }

            int degreeOfFreedom = getTotalNumPoints() - alreadyAdded;
            while (degreeOfFreedom > 0) {
                int randomIndex = new Random().nextInt(numCars);
                if (randomIndex < breaks.size())
                    breaks.set(randomIndex, breaks.get(randomIndex) + 1);
                degreeOfFreedom --;
            }

            getBreaks().set(0, getBreaks().get(0) - 1);
            int sum = getBreaks().get(0);
            for (int i = 1; i < getBreaks().size(); i++) {
                sum += getBreaks().get(i);
                getBreaks().set(i, sum);
            }
        } else {
            // Both need to be considered.
            // Only minTour needs to be considered.
            int alreadyAdded = 0;
            int lastTour = bundles.get(bundles.size() - 1).size() > minTour ? bundles.get(bundles.size() - 1).size() : minTour;

            // Do initial loading for break values.
            for (int i = 0; i < numCars - Math.max(1, bundles.size()); i++) {
                breaks.add(minTour);
                alreadyAdded += minTour;
            }

            for (int i = numCars - Math.max(1, bundles.size()); i < numCars - 1; i++) {
                if (bundles.get(i - (numCars - bundles.size())).size() >= minTour) {
                    alreadyAdded += bundles.get(i - (numCars - bundles.size())).size();
                    breaks.add(0);
                } else {
                    alreadyAdded += minTour;
                    breaks.add(minTour - bundles.get(i - (numCars - bundles.size())).size());
                }
            }

            if (!bundles.isEmpty()) {
                alreadyAdded += Math.max(bundles.get(bundles.size() - 1).size(), minTour);
            } else {
                alreadyAdded += minTour;
            }

            int degreeOfFreedom = getTotalNumPoints() - alreadyAdded;
            while (degreeOfFreedom > 0) {
                while (true) {
                    int randomIndex = new Random().nextInt(numCars);
                    if (randomIndex >= breaks.size() && lastTour < maxTour) {
                        lastTour ++;
                        break;
                    }

                    // 1. randomIndex in the range of breaks; 2.
                    else if (randomIndex < breaks.size() && ((randomIndex < numCars - bundles.size() && breaks.get(randomIndex) < maxTour) ||
                            (randomIndex >= numCars - bundles.size() && breaks.get(randomIndex) +
                                    bundles.get(randomIndex - (numCars - bundles.size())).size() < maxTour))) {
                        breaks.set(randomIndex, breaks.get(randomIndex) + 1);
                        break;
                    }
                }
                degreeOfFreedom --;
            }

            getBreaks().set(0, getBreaks().get(0) - 1);
            int sum = getBreaks().get(0);
            for (int i = 1; i < getBreaks().size(); i++) {
                sum += getBreaks().get(i);
                getBreaks().set(i, sum);
            }

            System.out.print("");
        }
    }

    private int getTotalNumPoints() {
        int sum = 0;
        if (hasBundles()) {
            for (List<SimplifiedTMSOrder> orddds : bundles) {
                sum += orddds.size();
            }
        }

        return sum + points.size();
    }

    public List<SimplifiedTMSOrder> getPoints() {
        return points;
    }

    public void setPoints(List<SimplifiedTMSOrder> points) {
        this.points = points;
    }

    public List<Integer> getBreaks() {
        return breaks;
    }

    public void setBreaks(List<Integer> breaks) {
        this.breaks = breaks;
    }

    public List<List<SimplifiedTMSOrder>> getBundles() {
        return bundles;
    }
}
