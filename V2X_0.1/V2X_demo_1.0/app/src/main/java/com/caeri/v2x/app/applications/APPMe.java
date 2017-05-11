package com.caeri.v2x.app.applications;

import android.util.Log;

import com.caeri.v2x.ldm.Me;
import com.caeri.v2x.util.ConfigurationBean;

import java.util.ArrayList;
import java.util.HashMap;

import static android.content.ContentValues.TAG;
import static com.caeri.v2x.util.Constants.CLASS_ID_ME_FROM_LDM;

/**
 * Created by geniu on 2017/5/9.
 */

public class APPMe extends Application {
    //应用类别 0-Me应用 1-安全类 2-效率类 3-信息类
    private static final String appType = "0";
    Me me;  //自身车辆信息(包括sv,sv所在link，lane，choosePhase，movement)
    public APPMe(ConfigurationBean.APPBean.ThresholdBean threshold) {

    }
    @Override
    public ArrayList<HashMap<String, String>> trigger() {
        ArrayList<HashMap<String, String>> toUIList = new ArrayList<>();
        HashMap<String, String> toUIMap = new HashMap<>();
        if (me.getVehicle() != null) {
            toUIMap.put("type", appType);
            toUIMap.put("longitude", String.valueOf(me.getVehicle().getLongitude()));
            toUIMap.put("latitude", String.valueOf(me.getVehicle().getLatitude()));
            toUIMap.put("NSEW", new String(me.getVehicle().getNSEW()));
            toUIMap.put("speed", String.valueOf(Math.floor(me.getVehicle().getSpeed())));
            toUIMap.put("speAngle", String.valueOf(me.getVehicle().getSpeAngle()));
            toUIMap.put("acceleration", String.valueOf(me.getVehicle().getAcceleration()));
            toUIMap.put("accAngle", String.valueOf(me.getVehicle().getAccAngle()));
            toUIMap.put("vehicleType1", String.valueOf(me.getVehicle().getVehicleType1()));
            toUIMap.put("vehicleType2", String.valueOf(me.getVehicle().getVehicleType2()));
            toUIMap.put("vehicleType3", String.valueOf(me.getVehicle().getVehicleType3()));
            toUIMap.put("CANInfo", new String(me.getVehicle().getCANinfo()));
            toUIMap.put("vehiclePlate", new String(me.getVehicle().getVehiclePlate()));
        }
        if (me.getLink() != null) {
            toUIMap.put("LinkName", new String(me.getLink().getName()));
        }
        if (me.getLane() != null) {
            toUIMap.put("laneNum", String.valueOf(me.getLink().getLaneNum()));
            toUIMap.put("laneIndex", String.valueOf(me.getLane().getIndex()));
        }
        toUIList.add(toUIMap);
        return toUIList;
    }

    @Override
    public void setAllParameter(HashMap<String, ArrayList<Object>> pubMap, HashMap<String, ArrayList<Object>> meMap) {
        me =(Me) meMap.get(CLASS_ID_ME_FROM_LDM).get(0);

    }
}
