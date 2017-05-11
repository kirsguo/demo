package com.caeri.v2x.app.applications;

import android.util.Log;

import com.caeri.v2x.comm.BSMSign;
import com.caeri.v2x.ldm.Me;
import com.caeri.v2x.util.ConfigurationBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import static android.content.ContentValues.TAG;
import static com.caeri.v2x.util.Constants.*;

/**
 * o
 * Created by JeRome on 2017/4/25.
 */

public class APPTrafficSign extends APPEfficiency {
    //应用类别 0-Me应用 1-安全类 2-效率类 3-信息类
    private static final String appType = "2";
    //关于交通信号的发布消息
    HashMap<String, ArrayList<Object>> pubMsg;
    //关于Me
    Me me;


    public APPTrafficSign(ConfigurationBean.APPBean.ThresholdBean threshold) {
    }

    @Override
    public ArrayList<HashMap<String, String>> trigger() {

        ArrayList<Object> signs = pubMsg.get(CLASS_ID_BSMSIGN);
        if (signs != null) {
            ArrayList<HashMap<String, String>> toUIList = new ArrayList<>();
            HashMap<String, String> toUIMap = new HashMap<>();
            for (Object o : signs) {

                BSMSign sign = (BSMSign) o;
                byte signType = sign.getSignType();
                switch (triggerSign(sign)) {
                    case 1:
                        toUIMap.put("signType", String.valueOf(signType));
                        System.out.println("接收到交通标志"+ signType);
                    default:
                        break;
                }

            }
            if (toUIMap != null) {
                toUIMap.put("type", appType);
                toUIList.add(toUIMap);
            }
            return toUIList;
        }
        return null;
    }

    private int triggerSign(BSMSign sign) {
        //触发点经纬度
        double posLon = sign.getPosLon();
        double posLat = sign.getPosLat();
        //车辆经纬度
        double x = me.getVehicle().getLongitude();
        double y = me.getVehicle().getLatitude();
//        触发半径
        byte radius = sign.getRadius();
//        车辆距离触发点距离
        double distance = getDistance(x, y, posLon, posLat);
        Log.i(TAG, "触发半径"+radius);
        Log.i(TAG, "车辆距离出发点距离"+distance);
        //利用BigDecimal来比较double
        BigDecimal b1 = BigDecimal.valueOf(radius);
        BigDecimal b2 = BigDecimal.valueOf(distance);
        if (b1.compareTo(b2) < 0) {
            return 0;    //触发半径小于车辆距离出发点，应用不触发，返回0
        } else {
            return 1;    //触发半径大于车辆距离出发点，应用不触发，返回1
        }

    }

    @Override
    public void setAllParameter(HashMap<String, ArrayList<Object>> pubMap, HashMap<String, ArrayList<Object>> meMap) {
        pubMsg = pubMap;
        me = (Me) meMap.get(CLASS_ID_ME_FROM_LDM).get(0);
    }

    //     通过两点的经纬度获取距离(单位：米)
    private static double EARTH_RADIUS = 6378.137;

    private static double getDistance(double lng1, double lat1, double lng2,
                                      double lat2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d;
        s = s * 1000;
        return s;
    }
}
