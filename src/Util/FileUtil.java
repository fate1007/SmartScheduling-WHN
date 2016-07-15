package Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BML-KF on 7/14/2016.
 */
public class FileUtil {
    private static String filePattern = "C:\\Users\\BML-KF\\Desktop\\Algorithms\\LBResults\\LBResult_%s.json";
    public static void saveResultsToFile(TMSRoutePlan finalPlan) {
        JSONObject resultObj = new JSONObject();
        resultObj.put("modifiedTime", System.currentTimeMillis());
        resultObj.put("breaks", finalPlan.getBreaks());
        resultObj.put("points", finalPlan.getPoints());
        System.out.println(resultObj.toString());
        File fl = new File(String.format(filePattern, System.currentTimeMillis()));
        try {
            FileWriter fr = new FileWriter(fl);
            fr.write(resultObj.toString(4));
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TMSRoutePlan readPlanFromFile() throws IOException {
        File file = new File(filePattern);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        //noinspection ResultOfMethodCallIgnored
        fis.read(data);
        fis.close();
        String encodedString = new String(data, "UTF-8");
        JSONObject resultObj = new JSONObject(encodedString);
        List<SimplifiedTMSOrder> points = (List<SimplifiedTMSOrder>) resultObj.get("points");
        List<Integer> breaks = (List<Integer>) resultObj.get("breaks");
        return new TMSRoutePlan(points, breaks);
    }

    public static void exportDistanceMatrix(List<SimplifiedTMSOrder> allOrders) {
        long currentTime = System.currentTimeMillis();
        String fileName = "C:\\Users\\BML-KF\\Desktop\\Algorithms\\LBResults\\" + "DistanceMatrix_" + currentTime + ".lmh";
        try {
            PrintWriter pr = new PrintWriter(fileName);
            String header = "origin/dest";
            for (SimplifiedTMSOrder currentOrder : allOrders) {
                header += " ";
                header += String.format("%.6f, %.6f", currentOrder.getLatlon().getLat(),
                        currentOrder.getLatlon().getLon());
            }
            pr.println(header);
            pr.flush();
            for (int i = 0; i < allOrders.size(); i++) {
                SimplifiedTMSOrder currentDestination = allOrders.get(i);
                String currentRow = String.format("%.6f, %.6f", currentDestination.getLatlon().getLat(),
                        currentDestination.getLatlon().getLon());
                for (int j = 0; j < allOrders.size(); j++) {
                    SimplifiedTMSOrder currentOrigin = allOrders.get(j);
                    currentRow += " ";
                    try {
                        int drivingDistance = OpenStreetMap.getDrivingDistance(currentOrigin.getLatlon(),
                                currentDestination.getLatlon());
                        currentRow += drivingDistance;
                    } catch (Exception e) {
                        currentRow += "-1";
                    }
                }

                pr.println(currentRow);
                pr.flush();
            }

            pr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
