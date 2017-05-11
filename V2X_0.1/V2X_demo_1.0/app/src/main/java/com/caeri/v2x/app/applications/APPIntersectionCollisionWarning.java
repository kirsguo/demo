package com.caeri.v2x.app.applications;

import android.util.Log;

import com.caeri.v2x.comm.BSMVehicle;
import com.caeri.v2x.ldm.Me;
import com.caeri.v2x.util.ConfigurationBean;

import java.util.ArrayList;
import java.util.HashMap;

import static android.content.ContentValues.TAG;
import static com.caeri.v2x.util.Constants.*;

/**
 * Created by JeRome on 2017/4/18.
 */

    //          夹角公式
    //        设其中一个斜率是k1，另一个是k2，夹角是a
    //        则tana=|（k1-k2）/（1+k1k2）|

public class APPIntersectionCollisionWarning extends APPSafety {
    //应用类别 0-Me应用 1-安全类 2-效率类 3-信息类
    private static final String appType = "1";
    Me me;  //自身车辆信息(包括sv,sv所在link，lane，choosePhase，movement)
    private BSMVehicle sv, rv;      //主车sv: subject vehicle    远车rv: remote vehicle
    HashMap<String, ArrayList<Object>> pubMsg = new HashMap<>();  //    发布的消息，String代表订阅类ID，Object代表类实例

//    APP ID

    //    警告主车驾驶员防撞的最小时间(单位毫秒)
    private float DWTmin;
    //最大延迟时间(单位毫秒)
    private float MLT;
    //    最大的驾驶员反应时间(单位毫秒)
    private float MDRT;
    //    最大动作时间(单位毫秒)
    private float MAT;
    //    边缘时间(单位毫秒)
    private float Epsilon;

    /**
     * 在构造函数中，给条件变量赋值
     */
//    HashMap<String, Float>
    public APPIntersectionCollisionWarning(ConfigurationBean.APPBean.ThresholdBean threshold) {
//        this.threshold = threshold;
        String[] args = new String[5];
        args[0] = threshold.getArg1();
        args[1] = threshold.getArg2();
        args[2] = threshold.getArg3();
        args[3] = threshold.getArg4();
        args[4] = threshold.getArg5();
        for (String str: args) {
            String[] strs = str.split("_");
            switch (strs[0]) {
                case "DWTmin":
                    DWTmin = Float.valueOf(strs[1]);
                    break;
                case "MLT":
                    MLT = Float.valueOf(strs[1]);
                    break;
                case "MDRT":
                    MDRT = Float.valueOf(strs[1]);
                    break;
                case "MAT":
                    MAT = Float.valueOf(strs[1]);
                    break;
                case "Epsilon":
                    Epsilon = Float.valueOf(strs[1]);
                    break;
            }
        }
    }
    public void setAllParameter(HashMap<String, ArrayList<Object>> pubMap, HashMap<String, ArrayList<Object>> meMap){
        me =(Me) meMap.get(CLASS_ID_ME_FROM_LDM).get(0);
        pubMsg = pubMap;
    }

    @Override
    public ArrayList<HashMap<String,String>> trigger() {
        HashMap<String,String> toUIMap = new HashMap();
        int collisionLevel = -1;
        //危险车辆所在位置 1-左边、2-右边 -1-初始化
        int lr = -1;
//        “2”代表类型为BSMVehicle类型
        sv = me.getVehicle();
        String RVToMe;
        ArrayList<Object> oVehicle = pubMsg.get(CLASS_ID_BSMVEHICLE);
        //获得主车经纬度、速度方向角、速度
        double longitude1 = sv.getLongitude();
        double latitude1 = sv.getLatitude();
        float speAngle1 = sv.getSpeAngle();
        float speed1 = sv.getSpeed();
        //主车静止，不提供警报
        if (speed1 != 0) {
            ArrayList<HashMap<String, String>> toUIList = new ArrayList<>();
            for (Object a : oVehicle) {
                rv = (BSMVehicle) a;
                //获得远车经纬度、速度方向角、速度
                double longitude2 = rv.getLongitude();
                double latitude2 = rv.getLatitude();
                float speAngle2 = rv.getSpeAngle();
                float speed2 = rv.getSpeed();
                //远车静止，不做判断
                if (speed2 == 0) {
                    continue;
                }
                //获取碰撞点经纬度
                double[] collisionPointXY = calculateCollisionPointXY(longitude1, latitude1, speAngle1, longitude2, latitude2, speAngle2);

                //主车到碰撞点向量：
                double[] svVector = {collisionPointXY[0] - longitude1, collisionPointXY[1] - latitude1};
                //远车到碰撞点向量：
                double[] rvVector = {collisionPointXY[0] - longitude2, collisionPointXY[1] - latitude2};
                int dic = leftOrright(svVector, rvVector);
                switch (dic) {
                    case 1:
                        lr = 2;
                        break;
                    case -1:
                        lr = 1;
                        break;
                }
                //判断碰撞点是否在行驶方向
                if (collisionPointIsRight(svVector, speAngle1, rvVector, speAngle2)) {
                    //A车离碰撞点的距离
                    double distanceA2CP = getDistance(longitude1, latitude1, collisionPointXY[0], collisionPointXY[1]);
                    //B车离碰撞点的距离
                    double distanceB2CP = getDistance(longitude2, latitude2, collisionPointXY[0], collisionPointXY[1]);
                    Log.i(TAG,"car1和碰撞点的距离是：" + distanceA2CP + "米");
                    Log.i(TAG,"car1和碰撞点的距离是：" + distanceB2CP + "米");
                    if (distanceA2CP != 0.0 && distanceB2CP != 0.0) {
                        //时间以毫秒为单位
                        float time = (float) Math.abs(distanceA2CP / (speed1 * 1000 / 3600) - distanceB2CP / (speed2 * 1000 / 3600));
                        Log.i(TAG,"两车到达碰撞点时间差为：" + time);
                        if (time >= 0 && time < 3) {
                            collisionLevel = 1;
                            Log.i(TAG,"发布碰撞预警");
                        } else if (time > 3 && time < 5) {
                            collisionLevel = 2;
                            Log.i(TAG,"告知道路危险");
                        } else if (time > 5 && time < 10) {
                            collisionLevel = 3;
                            Log.i(TAG,"在主车显示屏上显示周边车辆");
                        } else
                            Log.i(TAG,"请正常行驶");
                    } else {
                        Log.i(TAG,"数据异常");
                    }
                }
            }
            if (collisionLevel != -1 && lr != -1) {
                toUIMap.put("type", appType);
                toUIMap.put("collisionLevel", String.valueOf(collisionLevel));
                if (lr == 1) {
                    toUIMap.put("RVToMe", "left");
                    Log.i(TAG,"碰撞车辆在左边道路");
                } else {
                    toUIMap.put("RVToMe", "right");
                    Log.i(TAG,"碰撞车辆在右边道路");
                }
            }
            toUIList.add(toUIMap);
            return toUIList;
        }
        else {
            return null;
        }
    }

    private int leftOrright(double[] svVector ,double[] rvVector) {
        //        利用向量的叉积公式，判断两个向量的方位
        double temp = (svVector[0] * rvVector[1] - svVector[1] * rvVector[0]);
        if (temp > 0) {
            return 1;
        }else
            return -1;
    }
    //判断碰撞点是否在两车行驶前方
    boolean collisionPointIsRight(double[] svVector,double speAngle1, double[] rvVector,double speAngle2) {
        //正北基准向量
        double[] north = {0, 1};
        double angleSv = getRotateAngle(north[0], north[1], svVector[0], svVector[1]);
//        System.out.println(angleSv);
        double angleRv = getRotateAngle(north[0], north[1], rvVector[0], rvVector[1]);
//        System.out.println(angleRv);
        if (Math.abs(speAngle1 - angleSv) < 10 && Math.abs(speAngle2 - angleRv) < 5) {
            return true;
        }else{
            return false;
        }
    }
    //计算两个向量顺时针夹角，基准向量为x1,y1
    double getRotateAngle(double x1, double y1, double x2, double y2)
    {
        final double epsilon = 1.0e-6;
        final double nyPI = Math.acos(-1.0);
        double dist, dot, degree, angle;

        // normalize
        dist = Math.sqrt(x1 * x1 + y1 * y1);
        x1 /= dist;
        y1 /= dist;
        dist = Math.sqrt( x2 * x2 + y2 * y2 );
        x2 /= dist;
        y2 /= dist;
        // dot product
        dot = x1 * x2 + y1 * y2;
        if ( Math.abs(dot-1.0) <= epsilon)
            angle = 0.0;
        else if ( Math.abs(dot+1.0) <= epsilon )
            angle = nyPI;
        else {
            double cross;

            angle = Math.acos(dot);
            //cross product
            cross = x1 * y2 - x2 * y1;
            // vector p2 is clockwise from vector p1
            // with respect to the origin (0.0)
            if (cross > 0 ) {
                angle = 2 * nyPI - angle;
            }
        }
        degree = angle *  180.0 / nyPI;
        return degree;
    }
    //计算碰撞点坐标
    private static double[] calculateCollisionPointXY(double longitude1, double latitude1, float speAngle1, double longitude2, double latitude2, float speAngle2) {
        //下标0代表碰撞点经度，1代表纬度
        double[] collisionPointXY = new double[2];
        //k,b分别是车辆行驶轨迹延伸线方程y=kx+b的常量
        double k1, b1, k2, b2;
        //x,y分别代表经度和纬度
        double x1 = longitude1;
        double y1 = latitude1;
        double x2 = longitude2;
        double y2 = latitude2;
        k1 = Math.tan(Math.toRadians(speAngle1));
        k2 = Math.tan(Math.toRadians(speAngle2));
        b1 = y1 - k1 * x1;
        b2 = y2 - k2 * x2;
        collisionPointXY[0] = (b2 - b1) / (k1 - k2);
        collisionPointXY[1] = k1 * collisionPointXY[0] + b1;
//        System.out.println("car1速度方向角为："+Math.toDegrees(speAngle1)+",k = "+k1+", b = "+b1);
//        System.out.println("car2速度方向角为："+Math.toDegrees(speAngle2)+", k = "+k2+", b = "+b2);
        Log.i(TAG,"car1经度：" + longitude1 + "  纬度：" + latitude1);
        Log.i(TAG,"car2经度：" + longitude2 + "  纬度" + latitude2);
        Log.i(TAG,"car1和car2碰撞点经度：" + collisionPointXY[0] + "  纬度：" + collisionPointXY[1]);
        return collisionPointXY;
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
