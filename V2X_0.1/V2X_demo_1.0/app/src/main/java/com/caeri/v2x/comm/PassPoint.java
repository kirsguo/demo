package com.caeri.v2x.comm;

/**
 * Created by wind on 2017/4/6.
 */
public class PassPoint {
    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public byte[] getNSEW() {
        return NSEW;
    }

    double longitude;//经过的点的经度
    double latitude;//经过的点的纬度
    byte[] NSEW=new byte[2];//经纬度扩展
}