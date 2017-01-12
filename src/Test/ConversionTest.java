package Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import Util.CoordinateUtil;
import Util.LatLon;
import Util.SimplifiedTMSOrder;

/**
 * Created by BML-KF on 7/13/2016.
 */
public class ConversionTest {
    public static final List<LatLon> monitored = new ArrayList<>();
    public static void testCoordinateConversion(List<SimplifiedTMSOrder> coordinates) {
		String filePattern = "E:\\American study life in poly\\bml\\output\\CoordinateResults.txt";
        PrintWriter pr;
        try {
            pr = new PrintWriter(filePattern);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        for (SimplifiedTMSOrder sfo : coordinates) {
            LatLon orderLL = sfo.getLatlon();
            LatLon converted = CoordinateUtil.baiduToOpenMap(orderLL);
            monitored.add(converted);
            pr.println(String.format("BaiduLat: %s, BaiduLon: %s, OSMLat: %.6f, OSMLon: %.6f", orderLL.getLat(),
                    orderLL.getLon(), converted.getLat(), converted.getLon()));
            pr.println(String.format("%s, %s; %.6f, %.6f", orderLL.getLat(), orderLL.getLon(), converted.getLat(), converted.getLon()));
            pr.flush();
        }

        pr.close();
    }
}
