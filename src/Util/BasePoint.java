package Util;

import java.io.Serializable;

public class BasePoint implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -5114511533711802985L;

    private String id;

    private String baidu_latitude;

    private String baidu_longitude;

    private String osm_latitude;

    private String osm_longitude;

    private String pointDescription;

    public BasePoint(String baiduLat, String baiduLon) {
        this.baidu_latitude = baiduLat;
        this.baidu_longitude = baiduLon;
    }

    public BasePoint(String baiduLat, String baiduLon, String osmLat, String osmLon) {
        this.baidu_latitude = baiduLat;
        this.baidu_longitude = baiduLon;
        this.osm_latitude = osmLat;
        this.osm_longitude = osmLon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPointDescription() {
        return pointDescription;
    }

    public void setPointDescription(String pointDescription) {
        this.pointDescription = pointDescription;
    }

    public String getBaidu_latitude() {
        return baidu_latitude;
    }

    public void setBaidu_latitude(String baidu_latitude) {
        this.baidu_latitude = baidu_latitude;
    }

    public String getBaidu_longitude() {
        return baidu_longitude;
    }

    public void setBaidu_longitude(String baidu_longitude) {
        this.baidu_longitude = baidu_longitude;
    }

    public String getOsm_latitude() {
        return osm_latitude;
    }

    public void setOsm_latitude(String osm_latitude) {
        this.osm_latitude = osm_latitude;
    }

    public String getOsm_longitude() {
        return osm_longitude;
    }

    public void setOsm_longitude(String osm_longitude) {
        this.osm_longitude = osm_longitude;
    }

}
