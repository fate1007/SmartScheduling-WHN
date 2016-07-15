package Util;

import java.io.Serializable;

public class GeoFencePoint implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -637517560262453479L;

    private String id;

    private String latitude;

    private String longitude;

    private int fenceOrder;

    private GeoFence ownerFence;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public GeoFence getOwnerFence() {
        return ownerFence;
    }

    public void setOwnerFence(GeoFence ownerFence) {
        this.ownerFence = ownerFence;
    }

    public int getFenceOrder() {
        return fenceOrder;
    }

    public void setFenceOrder(int fenceOrder) {
        this.fenceOrder = fenceOrder;
    }
}
