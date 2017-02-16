package kmeans;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class SimplifiedTMSOrder implements Clusterable {
	private String addressId;
    private LatLon latlon;
    private String estimatedArrivingTime;
	private int duplicate;
	private double volume;

    public LatLon getLatlon() {
        return latlon;
    }

	public void setLatlon(LatLon latlon) {
        this.latlon = latlon;
    }

	public SimplifiedTMSOrder(LatLon ll) {
        this.latlon = ll;
		this.duplicate = 1;
		this.volume = 0.;
    }

	public SimplifiedTMSOrder(LatLon ll, double volume) {
		this.latlon = ll;
		this.duplicate = 1;
		this.volume = volume;
	}

	public String getEstimatedArrivingTime() {
		return this.estimatedArrivingTime;
	}

	public void setEstimatedArrivingTime(String estimatedArrivingTime) {
		this.estimatedArrivingTime = estimatedArrivingTime;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume += volume;
	}

	public String getAddressId() {
		return addressId;
	}

	public void setAddressId(String addressId) {
		this.addressId = addressId;
	}

    @Override
    public double[] getPoint() {
        return new double[]{ latlon.getLat(), latlon.getLon() };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
		result = prime * result + (addressId == null ? 0 : addressId.hashCode());
		result = prime * result + ((latlon == null) ? 0 : latlon.hashCode());
		result = (int) (prime * result + volume);
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
		if (addressId == null) {
			if (other.addressId != null) {
				return false;
			}
		} else if (!addressId.equals(other.addressId)) {
			return false;
		}
		if (latlon == null) {
			if (other.latlon != null)
                return false;
		} else if (!latlon.equals(other.latlon))
            return false;
		return this.volume == other.volume;
    }

	public int getDuplicate() {
		return duplicate;
	}

	public void setDuplicate(int duplicate) {
		this.duplicate = duplicate;
	}

	public void increaseDuplicate() {
		this.duplicate++;
	}
}
