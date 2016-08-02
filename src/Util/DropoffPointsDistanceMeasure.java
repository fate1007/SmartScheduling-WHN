package Util;

import Test.ConversionTest;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.DistanceMeasure;

import java.text.DecimalFormat;

/**
 * Utility for measuring distance between two dropoff points.
 * @author Kelvin Liu
 *
 */
public class DropoffPointsDistanceMeasure implements DistanceMeasure {

    private static final long serialVersionUID = 8795527049101845368L;
    private static boolean USE_DRIVING_DIST = false;
    private static boolean USE_STRICT_MATH = true;
    private static boolean NEVER_USE_SERVER_UNLESS_TOLD_TO = true;

    @Override
    public boolean compare(double arg0, double arg1) {
        // TODO Auto-generated method stub
        return arg0 < arg1;
    }

    @Override
    public double getMaxValue() {
        // TODO Auto-generated method stub
        return 100000;
    }

    @Override
    public double getMinValue() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double measure(Instance arg0, Instance arg1) {
        // TODO Auto-generated method stub
        return measurePoint(new LatLon(arg0.value(0), arg0.value(1)), new LatLon(arg1.value(0), arg1.value(1)));
    }

    public static double measurePoint(LatLon ll1, LatLon ll2) {
        if (USE_DRIVING_DIST) {
            try {
                ll1 = CoordinateUtil.baiduToOpenMap(ll1);
                ll2 = CoordinateUtil.baiduToOpenMap(ll2);
                DecimalFormat df = new DecimalFormat("######0.000000");
                ll1 = new LatLon(Double.parseDouble(df.format(ll1.getLat())), Double.parseDouble(df.format(ll1.getLon())));
                ll2 = new LatLon(Double.parseDouble(df.format(ll2.getLat())), Double.parseDouble(df.format(ll2.getLon())));
                return OpenStreetMap.getDrivingDistance(ll1, ll2);
            } catch (Exception e) {
//                if (ConversionTest.monitored.contains(ll1) && ConversionTest.monitored.contains(ll2))
                e.printStackTrace();
                OpenStreetMap.configureCalculationSchema();
                double lineDist = GeoPolygon.getEarthDistance(ll1.getLat(), ll1.getLon(), ll2.getLat(), ll2.getLon());
                if (lineDist < 1)
                    return lineDist * 1.1;
                else if (lineDist < 5)
                    return lineDist * 1.4;
                else
                    return lineDist * 1.3;
            }
        } else if (USE_STRICT_MATH) {
            return GeoPolygon.getEarthDistance(ll1.getLat(), ll1.getLon(), ll2.getLat(), ll2.getLon());
        } else {
            double lineDist = GeoPolygon.getEarthDistance(ll1.getLat(), ll1.getLon(), ll2.getLat(), ll2.getLon());
            if (lineDist < 1)
                return lineDist * 1.1;
            else if (lineDist < 5)
                return lineDist * 1.4;
            else
                return lineDist * 1.3;
        }
    }

    public static double measurePoint(SimplifiedTMSOrder order1, SimplifiedTMSOrder order2) {
        return measurePoint(order1.getLatlon(), order2.getLatlon());
    }

    public static void disableServerAccess() {
        USE_DRIVING_DIST = false;
    }

    public static void enableServerAccess() {
        if (!NEVER_USE_SERVER_UNLESS_TOLD_TO)
            USE_DRIVING_DIST = true;
    }
}
