package Util;

import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * Compact representation for TMS orders.
 * @author Kelvin Liu
 *
 */
public class SimplifiedTMSOrder implements Clusterable {
    private LatLon latlon;
    private TMSOrderLabel orderLabel;
    private String estimatedArrivingTime;
    private double volumn;

    public LatLon getLatlon() {
        return latlon;
    }

    public void setLatlon(LatLon latlon) {
        this.latlon = latlon;
    }

    public SimplifiedTMSOrder(TMSOrderLabel orderLabel, LatLon ll) {
        this.latlon = ll;
        this.orderLabel = orderLabel;
    }

    public TMSOrderLabel getOrderLabel() {
        return orderLabel;
    }

    public void setOrderLabel(TMSOrderLabel orderLabel) {
        this.orderLabel = orderLabel;
    }

    public void setEstimatedArrivingTime(String estimatedArrivingTime) {
        this.estimatedArrivingTime = estimatedArrivingTime;
    }

	public String getEstimatedArrivingTime() {
		return estimatedArrivingTime;
	}

    @Override
    public double[] getPoint() {
        return new double[]{ latlon.getLat(), latlon.getLon() };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((orderLabel == null) ? 0 : orderLabel.hashCode());
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
        SimplifiedTMSOrder other = (SimplifiedTMSOrder) obj;
        if (orderLabel == null) {
            if (other.orderLabel != null)
                return false;
        } else if (!orderLabel.equals(other.orderLabel))
            return false;
        return true;
    }
}
