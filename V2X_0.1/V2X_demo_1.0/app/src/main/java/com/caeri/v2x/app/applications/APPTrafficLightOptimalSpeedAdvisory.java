package com.caeri.v2x.app.applications;

import android.util.Log;

import com.caeri.v2x.comm.BSMMovement;
import com.caeri.v2x.comm.BSMTrafficLight;
import com.caeri.v2x.comm.MovePhase;
import com.caeri.v2x.comm.Phase;
import com.caeri.v2x.ldm.Me;
import com.caeri.v2x.util.ConfigurationBean;

import java.util.ArrayList;
import java.util.HashMap;

import static android.content.ContentValues.TAG;
import static com.caeri.v2x.util.Constants.*;

/**
 * Created by JeRome on 2017/4/25.
 */

public class APPTrafficLightOptimalSpeedAdvisory extends APPEfficiency {
    //应用类别 0-Me应用 1-安全类 2-效率类 3-信息类
    private static final String appType = "2";
    HashMap<String, ArrayList<Object>> pubMsg;    //从LDM收到的该应用的发布消息
    private BSMTrafficLight trafficLight; //交通灯
    private Me me; //车辆自身信息
    double[] meLinkVector = new double[2]; //me所在link向量
    public APPTrafficLightOptimalSpeedAdvisory(ConfigurationBean.APPBean.ThresholdBean threshold) {

    }

    public void setAllParameter(HashMap<String, ArrayList<Object>> pubMap, HashMap<String, ArrayList<Object>> meMap){
        me =(Me) meMap.get(CLASS_ID_ME_FROM_LDM).get(0);
        pubMsg = pubMap;
    }

    @Override
    public ArrayList<HashMap<String,String>> trigger() {
        trafficLight =  (BSMTrafficLight) pubMsg.get(CLASS_ID_BSMTRAFFICLIGHT).get(0);
        byte linkFromNodeLocalID = me.getLink().getFromNodeLocalID(); // Me所在Link的起点ID
        byte linkToNodeLocalID = me.getLink().getToNodeLocalID();// Me所在Link的终点ID
        byte trafficLightCenterNodeLocalID = trafficLight.getCenterNodeLocalID();
        meLinkVector[0] = me.getNodes().get(linkToNodeLocalID).getLongitude()-me.getNodes().get(linkFromNodeLocalID).getLongitude();
        meLinkVector[1] = me.getNodes().get(linkToNodeLocalID).getLatitude()-me.getNodes().get(linkFromNodeLocalID).getLatitude();
        boolean left = false;
        boolean head = false;
        boolean right = false;
        MovePhase[] movePhases = me.getChoosePhase().getMovePhase();
        Phase[] phases = trafficLight.getPhase();
//        确认车辆所在link的终点node就是交通灯的node
        if (linkToNodeLocalID == trafficLightCenterNodeLocalID) {
            HashMap<String, String> toUIMap = new HashMap<>();
            ArrayList<HashMap<String, String>> toUIList = new ArrayList<>();
//            得到符合当前车辆行驶的movement
            for (BSMMovement move: me.getMovements()) {
                if (move.getFromNodeLocalID() == linkFromNodeLocalID && move.getCenterNodeLocalID() == linkToNodeLocalID) {
//                    这条movement的向量
                    byte fromNodeLocalID = move.getFromNodeLocalID();
                    byte centerNodeLocalID = move.getCenterNodeLocalID();
                    byte toNodeLocalID = move.getToNodeLocalID();
                    byte phaseID = -1;
                    byte status = -1;
                    short timeleft = -1;
                    short green = -1;
                    short yellow = -1;
                    short red = -1;
                    double x= me.getNodes().get(toNodeLocalID).getLongitude() - me.getNodes().get(centerNodeLocalID).getLongitude();
                    double y= me.getNodes().get(toNodeLocalID).getLatitude() - me.getNodes().get(centerNodeLocalID).getLatitude();
                    //判断该movement方向
                    double angle = Math.toDegrees(Math.acos((meLinkVector[0] * x + meLinkVector[1] * y) /
                            (Math.sqrt(Math.pow(meLinkVector[0], 2) + Math.pow(meLinkVector[1], 2)) * Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)))));
                    int direction = 0;
                    if (angle < 10) { //如果夹角小于10度，认为这是一条直行路
                        boolean test = head;
                        if (head == false) {
                            direction = 2; //2代表直行
                            head = true;
                        } else {
                            continue;
                        }
                    } else { //如果夹角大于10度，则这条路则不是一个直行方向，需要判断是左转还是右转
                        int i = leftOrright(x,y);
                        if (i == -1) { //-1代表右转
                            if (right == false) {
                                direction = 3;
                                right = true;
                            } else {
                                continue;
                            }
                        }
                        if (i == 1) {
                            if (left == false) {
                                direction = 1;
                                left = true;
                            } else {
                                continue;
                            }
                        }
                    }
                    //确定movement的movePhase
                    for (MovePhase movePhase : movePhases) {
                        if (movePhase.getFromNodeLocalID() == move.getFromNodeLocalID() && movePhase.getToNodeLocalID() == move.getToNodeLocalID()) {
                            phaseID = movePhase.getPhaseID();
                            break;
                        }
                    }
                    //确定movePhase的phase
                    for (Phase phase : phases) {
                        if (phase.getPhaseID() == phaseID) {
                            status = phase.getStatus();
                            timeleft = phase.getTimeLeft();
                            green = phase.getGreen();
//                            yellow = phase.getYellow();
                            red = phase.getRed();
                            break;
                        }
                    }
                    float distance = (float)getDistance(me.getVehicle().getLongitude(), me.getVehicle().getLatitude(),
                            me.getLink().getStopLongitude(), me.getLink().getStopLatitude());
                    if (status != -1 && timeleft != -1 && direction != -1) {
                        switch (status) {
//                            当前状态：1-绿灯、2-黄灯、3-红灯、4-红闪、5-绿闪、6-黄闪、7-灯全灭
                            case 1: // 绿灯
                            case 5: // 绿闪
                            {
                                float maxSpeed = 0;
                                float minSpeed = 0;
                                try {
                                    maxSpeed = Math.round(me.getLane().getSpeedLimit());
                                    minSpeed = Math.round(((distance / timeleft) * 3.6f));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                switch (direction) {
//                                        1-左转、2-直行、3-右转
                                    case 1: {
                                        toUIMap.put("leftStatus", String.valueOf(status));
                                        toUIMap.put("leftTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + direction + "m");
                                        Log.i(TAG,"绿灯剩余时间为：" + timeleft + "s");
                                        if (maxSpeed >= minSpeed) {
                                            toUIMap.put("leftMaxSpeed", String.valueOf(maxSpeed));
                                            toUIMap.put("leftMinSpeed", String.valueOf(minSpeed));
                                            Log.i(TAG,"左转最大速度：" + maxSpeed + "km/h");
                                            Log.i(TAG,"左转最低速度：" + minSpeed + "km/h");
                                        } else {
                                            toUIMap.put("leftMaxSpeed", "-");
                                            toUIMap.put("leftMinSpeed", "-");
                                            Log.i(TAG,"左转最大速度： -");
                                            Log.i(TAG,"左转最低速度： -");
                                        }
                                    }
                                    break;
                                    case 2: {
                                        toUIMap.put("headStatus", String.valueOf(status));
                                        toUIMap.put("headTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + direction + "m");
                                        Log.i(TAG,"绿灯剩余时间为：" + timeleft + "s");
                                        if (maxSpeed >= minSpeed) {
                                            toUIMap.put("headMaxSpeed", String.valueOf(maxSpeed));
                                            toUIMap.put("headMinSpeed", String.valueOf(minSpeed));
                                            Log.i(TAG,"直行最大速度：" + maxSpeed + "km/h");
                                            Log.i(TAG,"直行最低速度：" + minSpeed + "km/h");
                                        } else {
                                            toUIMap.put("headMaxSpeed", "-");
                                            toUIMap.put("headMinSpeed", "-");
                                            Log.i(TAG,"直行最大速度： -");
                                            Log.i(TAG,"直行最低速度： -");
                                        }

                                    }
                                    break;
                                    case 3: {
                                        toUIMap.put("rightStatus", String.valueOf(status));
                                        toUIMap.put("rightTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + direction + "m");
                                        Log.i(TAG,"绿灯剩余时间为：" + timeleft + "s");
                                        if (maxSpeed >= minSpeed) {
                                            toUIMap.put("rightMaxSpeed", String.valueOf(maxSpeed));
                                            toUIMap.put("rightMinSpeed", String.valueOf(minSpeed));
                                            Log.i(TAG,"右转最大速度：" + maxSpeed + "km/h");
                                            Log.i(TAG,"右转最低速度：" + minSpeed + "km/h");
                                        } else {
                                            toUIMap.put("rightMaxSpeed", "-");
                                            toUIMap.put("rightMinSpeed", "-");
                                            Log.i(TAG,"右转最大速度： -");
                                            Log.i(TAG,"右转最低速度： -");
                                        }

                                    }
                                    break;
                                    default:
                                        Log.i(TAG,"direction -1");
                                        break;
                                }
                            }
                            break;
                            case 2:  //黄灯
                            case 6:  //黄闪
                            {

                                float maxSpeed = 0;
                                float minSpeed = 0; //在黄灯和红灯过后，还能在下一个周期的绿灯能过去的建议速度
                                try {
                                    float limitSeepd = me.getLane().getSpeedLimit(); //当前lane限速
                                    float adviseSpeed = distance / (timeleft + red) * 3.6f; //在黄灯过后，下一个红灯之前不停车的建议速度
                                    maxSpeed = Math.round((adviseSpeed <= limitSeepd) ? adviseSpeed : limitSeepd);
                                    minSpeed = Math.round(distance / (timeleft + red + green) * 3.6f);
                                    if (minSpeed > limitSeepd) {
                                        minSpeed = limitSeepd;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                switch (direction) {
                                    case 1: {
                                        toUIMap.put("leftStatus", String.valueOf(status));
                                        toUIMap.put("leftTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + distance + "m");
                                        Log.i(TAG,"黄灯剩余时间为：" + timeleft + "s + 红灯秒数：" + red);
                                        toUIMap.put("leftMaxSpeed", String.valueOf(maxSpeed));
                                        toUIMap.put("leftMinSpeed", String.valueOf(minSpeed));
                                        Log.i(TAG,"左转最大速度：" + maxSpeed + "km/h");
                                        Log.i(TAG,"左转最低速度：" + minSpeed + "km/h");
                                    }
                                    break;
                                    case 2: {
                                        toUIMap.put("headStatus", String.valueOf(status));
                                        toUIMap.put("headTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + distance + "m");
                                        Log.i(TAG,"黄灯剩余时间为：" + timeleft + "s + 红灯秒数：" + red);
                                        toUIMap.put("headMaxSpeed", String.valueOf(maxSpeed));
                                        toUIMap.put("headMinSpeed", String.valueOf(minSpeed));
                                        Log.i(TAG,"直行最大速度：" + maxSpeed + "km/h");
                                        Log.i(TAG,"直行最低速度：" + minSpeed + "km/h");
                                    }
                                    break;
                                    case 3: {
                                        toUIMap.put("rightStatus", String.valueOf(status));
                                        toUIMap.put("rightTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + distance + "m");
                                        Log.i(TAG,"黄灯剩余时间为：" + timeleft + "s + 红灯秒数：" + red);
                                        toUIMap.put("rightMaxSpeed", String.valueOf(maxSpeed));
                                        toUIMap.put("rightMinSpeed", String.valueOf(minSpeed));
                                        Log.i(TAG,"右转最大速度：" + maxSpeed + "km/h");
                                        Log.i(TAG,"右转最低速度：" + minSpeed + "km/h");
                                    }
                                    break;
                                    default:
                                        Log.i(TAG,"direction -1");
                                        break;
                                }
                            }
                            break;
                            case 3: // 红灯
                            case 4: // 红闪
                            {

                                float maxSpeed = 0;
                                float minSpeed = 0; //在红灯过后，还能在下一个周期的绿灯能过去的建议速度
                                try {
                                    float limitSeepd = me.getLane().getSpeedLimit(); //当前lane限速
                                    float adviseSpeed = distance / timeleft * 3.6f; //在红灯不停车的最大建议速度
                                    maxSpeed = Math.round((adviseSpeed <= limitSeepd) ? adviseSpeed : limitSeepd);
                                    minSpeed = Math.round(distance / (timeleft + green) * 3.6f);
                                    if (minSpeed > limitSeepd) {
                                        minSpeed = limitSeepd;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                switch (direction) {
                                    case 1: {
                                        toUIMap.put("leftStatus", String.valueOf(status));
                                        toUIMap.put("leftTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + distance + "m");
                                        Log.i(TAG,"红灯剩余时间为：" + timeleft + "s");
                                        toUIMap.put("leftMaxSpeed", String.valueOf(maxSpeed));
                                        toUIMap.put("leftMinSpeed", String.valueOf(minSpeed));
                                        Log.i(TAG,"左转最大速度：" + maxSpeed + "km/h");
                                        Log.i(TAG,"左转最低速度：" + minSpeed + "km/h");

                                    }
                                    break;
                                    case 2: {
                                        toUIMap.put("headStatus", String.valueOf(status));
                                        toUIMap.put("headTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + distance + "m");
                                        Log.i(TAG,"红灯剩余时间为：" + timeleft + "s");
                                        toUIMap.put("headMaxSpeed", String.valueOf(maxSpeed));
                                        toUIMap.put("headMinSpeed", String.valueOf(minSpeed));
                                        Log.i(TAG,"直行最大速度：" + maxSpeed + "km/h");
                                        Log.i(TAG,"直行最低速度：" + minSpeed + "km/h");
                                    }
                                    break;
                                    case 3: {
                                        toUIMap.put("rightStatus", String.valueOf(status));
                                        toUIMap.put("rightTimeLeft", String.valueOf(timeleft));
                                        Log.i(TAG,"车辆距离停止线距离：" + distance + "m");
                                        Log.i(TAG,"红灯剩余时间为：" + timeleft + "s");
                                        toUIMap.put("rightMaxSpeed", String.valueOf(maxSpeed));
                                        toUIMap.put("rightMinSpeed", String.valueOf(minSpeed));
                                        Log.i(TAG,"右转最大速度：" + maxSpeed + "km/h");
                                        Log.i(TAG,"右转最低速度：" + minSpeed + "km/h");
                                    }
                                    break;
                                    default:
                                        Log.i(TAG,"direction -1");
                                        break;
                                }
                            }
                            break;
                            default: {
                                switch (direction) {
                                    case 1:
                                        toUIMap.put("leftStatus", "7");
                                        break;
                                    case 2:
                                        toUIMap.put("headStatus", "7");
                                        break;
                                    case 3:
                                        toUIMap.put("rightStatus", "7");
                                        break;
                                    default:
                                        break;
                                }

                            }

                        }
                    } else {
                        Log.i(TAG,"status"+status+" direction"+direction+" timeleft"+timeleft);
                    }

                }
            }
            if (left == false) {
                toUIMap.put("leftStatus", "7");
                Log.i(TAG,"没有左转");
            }
            if (head == false) {
                toUIMap.put("headStatus", "7");
                Log.i(TAG,"没有直行");
            }
            if (right == false) {
                toUIMap.put("rightStatus", "7");
                Log.i(TAG,"没有右转");
            }
            //        返回给UI的消息
            if (toUIMap != null) {
                toUIMap.put("type", appType);
            }
            toUIList.add(toUIMap);
            return toUIList;
        } else {
            Log.i(TAG,"交通灯不是当前道路行驶方向的路口");
            return null;
        }


    }

/**       利用向量的叉积公式，判断两个向量的方位 */
    private int leftOrright(double x ,double y) {
    //        利用向量的叉积公式，判断两个向量的方位
        double temp = (meLinkVector[0] * y - x * meLinkVector[1]);
        if (temp > 0) {
            return 1;
        }else
            return -1;
    }


    /**
     * 通过两点的经纬度获取距离(单位：米)
     */
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
