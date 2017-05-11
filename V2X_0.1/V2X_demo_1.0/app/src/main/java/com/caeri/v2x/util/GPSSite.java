package com.caeri.v2x.util;

import java.math.BigDecimal;

/**
 * GPSSite
 * <p>
 * Created on 4/29/029.
 *
 * @author Benjamin
 */

public final class GPSSite {
    public static final double EARTH_RADIUS = 6378.137;   // 地球半径，km
    private double latitude;
    private double longitude;
    private byte[] NSEW;

    public GPSSite() {
    }

    public GPSSite(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSSite(double latitude, double longitude, byte[] NSEW) {
        this.NSEW = NSEW;
        this.latitude = latitude;
        this.longitude = longitude;
        if (this.NSEW[0] == (byte) 'W') {
            this.longitude += 180;  // 西经加180
        }
        if (this.NSEW[1] == (byte) 'S') {
            this.latitude *= -1;    // 南纬为负数
        }
    }

    public byte[] getNSEW() {
        return NSEW;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * 根据经纬度求两点距离。
     *
     * @param mySite 起点
     * @param site   终点
     * @return distance km
     */
    public static double getDistance(GPSSite mySite, GPSSite site) {
        double radLat1 = Math.toRadians(mySite.getLatitude());
//                mySite.getLatitude() * Math.PI / 180;
        double radLat2 = Math.toRadians(site.getLatitude());
//                site.getLatitude() * Math.PI / 180;
        double a = radLat1 - radLat2;
        double b = Math.toRadians(mySite.getLongitude())-Math.toRadians(site.getLongitude());
//                mySite.getLongitude() * Math.PI / 180 - site.getLongitude() * Math.PI / 180;

        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s *= EARTH_RADIUS;
//        s = Math.Round(s * 10000) / 10000;
        return s;
    }

    /**
     * 根据GPS求点到线段的距离
     *
     * @param mySite   点
     * @param fromSite 线的起点
     * @param toSite   线的终点
     * @return distance 点到线的距离，单位km
     * @see <a href=http://blog.csdn.net/ufoxiong21/article/details/46487001>经纬坐标系中求点到线段距离的方法</a>
     */
    public static double getDistance(GPSSite mySite, GPSSite fromSite, GPSSite toSite) {
//        double distance = 0.0;
//        // 两点式直线方程转换成 Ax+By+C=0;    x:Latitude, y:Longitude
//        double A = fromSite.getLongitude() - toSite.getLongitude();
//        double B = toSite.getLatitude() - fromSite.getLatitude();
//        double C = toSite.getLongitude() * fromSite.getLatitude() - toSite.getLatitude() * fromSite.getLongitude();
//        distance = Math.abs(A * mySite.getLatitude() + B * mySite.getLongitude() + C)
//                / Math.sqrt(A * A + B * B);
        double a = getDistance(fromSite, toSite);
        double b = getDistance(toSite, mySite);
        double c = getDistance(mySite, fromSite);
//        BigDecimal bda = BigDecimal.valueOf(a * 10000);
//        BigDecimal bdb = BigDecimal.valueOf(b * 10000);
//        BigDecimal bdc = BigDecimal.valueOf(c * 10000);
//        BigDecimal bda2 = bda.pow(2);
//        BigDecimal bdb2 = bdb.pow(2);
//        BigDecimal bdc2 = bdc.pow(2);
//        if (bdb2.compareTo(bda2.add(bdc2)) >= 0) {
//            return c;
//        }
//        if (bdc2.compareTo(bda2.add(bdb2)) >= 0) {
//            return b;
//        }
        if (b * b >= a * a + c * c) {
            return c;
        }
        if (c * c >= a * a + b * b) {
            return b;
        }

        double p = (a + b + c) / 2.0;   // 周长的一半
        double s = Math.sqrt(p * (p - a) * (p - b) * (p - c));
        return 2 * s / a;
    }

    /**
     * 使用向量点积计算两条线段的夹角。
     *
     * @param origin1 线段1的起点
     * @param p1      线段1的终点
     * @param origin2 线段2的起点
     * @param p2      线段2的终点
     * @return 两条线段的夹角的弧度值。
     */
    public static double getRadians(GPSSite origin1, GPSSite p1, GPSSite origin2, GPSSite p2) {
        double x1 = p1.getLatitude() - origin1.getLatitude();
        double y1 = p1.getLongitude() - origin1.getLongitude();
        double x2 = p2.getLatitude() - origin2.getLatitude();
        double y2 = p2.getLongitude() - origin2.getLongitude();
        double module1 = Math.sqrt(x1 * x1 + y1 * y1);
        double module2 = Math.sqrt(x2 * x2 + y2 * y2);
        double dotProduct = x1 * x2 + y1 * y2;
        return Math.acos(dotProduct / (module1 * module2));
    }

    /**
     * 使用向量的叉积计算两条线段的顺逆时针关系。
     *
     * @param origin1 线段1的起点
     * @param p1      线段1的终点
     * @param origin2 线段2的起点
     * @param p2      线段2的终点
     * @return 1 if 线段1在线段2的顺时针方向（右边）；
     * -1 if 线段1在线段2的逆时针方向（左边）；
     * 0 if 线段1和线段2同向或逆向。
     */
    public static int getLeftOrRight(GPSSite origin1, GPSSite p1, GPSSite origin2, GPSSite p2) {
        double x1 = p1.getLatitude() - origin1.getLatitude();
        double y1 = p1.getLongitude() - origin1.getLongitude();
        double x2 = p2.getLatitude() - origin2.getLatitude();
        double y2 = p2.getLongitude() - origin2.getLongitude();
        double crossProduct = x1 * y2 - x2 * y1;
        BigDecimal b1 = BigDecimal.valueOf(crossProduct);
        BigDecimal b2 = BigDecimal.valueOf(0);
        if (b1.compareTo(b2) > 0) {
            return 1;
        } else if (b1.compareTo(b2) < 0) {
            return -1;
        } else {
            return 0;
        }
    }
}
