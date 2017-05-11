package com.caeri.v2x.app;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.caeri.v2x.app.applications.APPIntersectionCollisionWarning;
import com.caeri.v2x.app.applications.APPMe;
import com.caeri.v2x.app.applications.APPTrafficLightOptimalSpeedAdvisory;
import com.caeri.v2x.app.applications.APPTrafficSign;
import com.caeri.v2x.app.applications.Application;

import com.caeri.v2x.ldm.LDMService;
import com.caeri.v2x.util.ConfigurationBean;
import com.caeri.v2x.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.caeri.v2x.util.Constants.*;

/**
 * Created by JeRome on 2017/4/25.
 */

public class DispatcherService extends Service {
    /**
     * Dispatcher主要作用就是来协调应用与LDM交互，应用与UI交互
     *
     * 作为一个Service，在UI启动DispatcherService的时候，主动绑定LDMService，让LDM作为APP层的数据提供者
     *
     * Dispatcher主要分为两个阶段，在UI启动DispatcherService后，Dispatcher首先初始化，调用loadConfiguration()
     * 目的在于加载Json信息，其中包括有几个应用被激活，每个应用的订阅信息，还有每个应用的阈值。
     *
     * 在DispatcherService中，主要有三种数据结构：
     * //        Dispatcher -> LDM的订阅消息
     * 第一个String:代表ClassId
     * 第二个String:代表AppId
     * 第三个String:代表FilterCode
     * HashMap<String, HashMap<String, String>> subscribeMsg = new HashMap<>();
     *
     * //        LDM -> Dispatcher的发布消息
     * 第一个String:代表AppID
     * 第二个string:代表类id
     * Object:代表类的实例
     * HashMap<String, HashMap<String, ArrayList<Object>>> publishMsg = new HashMap<>();
     *
     * //        Dispatcher -> UI的消息
     * 第一个String:代表AppID
     * 第二个String:代表返回字段名
     * 第三个String:代表返回字段值
     * HashMap<String, ArrayList<HashMap<String, String>>> toUIList = new HashMap<>();
     *
     */

    //        订阅消息
    HashMap<String, HashMap<String, String>> subscribeMsg = new HashMap<>();
    //        发布消息
    HashMap<String, HashMap<String, ArrayList<Object>>> publishMsg = new HashMap<>();
    //        返回给UI的消息
    HashMap<String, ArrayList<HashMap<String, String>>> toUIList = new HashMap<>();

    //应用引用
    APPMe appMe;
    APPIntersectionCollisionWarning appIntersectionCollisionWarning;
    APPTrafficLightOptimalSpeedAdvisory appTrafficLightOptimalSpeedAdvisory;
    APPTrafficSign appTrafficSign;

    //接收到LDM发布的Message中，取出Me最新消息并备份(因为返回的发布消息中，Me应用和其他应用不同步，所以要自己保存一份Me应用)
    HashMap<String, ArrayList<Object>> meMap =new HashMap<>();

    private static final String TAG = "DispatcherService";
    private static final int ForMyself = -1;

    private Handler UIHandler;
    private DispatcherBinder mBinder = new DispatcherBinder();
    private HandlerThread dispatcherThread;

    //创建Dispatcher的handler
    private DispatcherHandler mHandler;
    final class DispatcherHandler extends Handler{
        public DispatcherHandler(Looper looper) {
            // 将Handler和looper绑定
            super(looper);
//            Log.d(TAG, "DispatcherHandler: "+Thread.currentThread().getId());
        }

        @Override
        public void handleMessage(Message msg) {
            //        声明应用引用
            HashMap<String,Application> appMap = new HashMap<>();
            Log.i(TAG, "handleMessage: "+msg.what);
//            Log.i(TAG, "CurrentThread in Dispatcher: "+Thread.currentThread().getId());
            // TODO:处理消息的应用逻辑
            switch (msg.what) {
                //接收LDM发布消息
                case NORMAL_MESSAGE:{
                    if (msg.obj != null) {
                        Log.i(TAG, "Dispatcher handleMessage");
                        publishMsg = (HashMap<String, HashMap<String, ArrayList<Object>>>) msg.obj;
                        //因为每个应用都会用到自身车辆信息，也就是应用V2XAPP_001的发布消息,所以现将它的发布消息先单独取出来，然后传给其他应用
                        meMap =  publishMsg.get(APP_ID_APPMe);
                        Set entries = publishMsg.entrySet();
                        if (entries != null && meMap != null) {
                            Iterator iterator = entries.iterator();
                            while (iterator.hasNext()) {
                                Map.Entry entry = (Map.Entry) iterator.next();
                                String key = (String) entry.getKey();
                                HashMap<String, ArrayList<Object>> map = (HashMap<String, ArrayList<Object>>) entry.getValue();
                                switch (key) {
                                    case APP_ID_APPMe:
                                        if (appMe != null) {
                                            //注：因为APP_ID_APPMe是作为一个应用，不用跟LDM订阅，但是LDM会跟Dispatcher发送这个类的消息
                                            //为了统一，这里给APPMe注入的参数map和meMap，一个是单独提出来应用Me发布消息，一个是遍历取出的，否则appMe继承application会出现无法实现问题(方法参数个数问题)
                                            appMe.setAllParameter(map, meMap);
                                            appMap.put(key, appMe);
                                        }
                                        break;
                                    case APP_ID_APPTrafficSign:
                                        if (appTrafficSign != null) {
                                            appTrafficSign.setAllParameter(map, meMap);
                                            appMap.put(key, appTrafficSign);
                                        }
                                        break;
                                    case APP_ID_APPIntersectionCollisionWarning:
                                        if (appIntersectionCollisionWarning != null) {
                                            appIntersectionCollisionWarning.setAllParameter(map, meMap);
                                            appMap.put(key, appIntersectionCollisionWarning);
                                        }
                                        break;
                                    case APP_ID_APPTrafficLightOptimalSpeedAdvisory:
                                        if (appTrafficLightOptimalSpeedAdvisory != null) {
                                            appTrafficLightOptimalSpeedAdvisory.setAllParameter(map, meMap);
                                            appMap.put(key, appTrafficLightOptimalSpeedAdvisory);
                                        }
                                        break;
                                }
                            }
                        }
                    }
                    //调用已发布应用的trigger
                    Set<String> keySets = appMap.keySet();
                    Iterator i = keySets.iterator();
                    while (i.hasNext()) {
                        String appID = i.next().toString();
                        ArrayList<HashMap<String, String>> list = appMap.get(appID).trigger();
                        if (list != null) {
                            toUIList.put(appID , list); //将应用返回的信息放入返回给UI的map中
                        }
                    }
                    //将toUIMap发送给UI
                    if (UIHandler != null) {
                        Message toUIMsg = Message.obtain();
                        toUIMsg.what = NORMAL_MESSAGE;
                        toUIMsg.obj = toUIList;
                        Log.i(TAG, "Dispatcher: "+toUIMsg.obj.toString());
                        UIHandler.sendMessage(toUIMsg);
                    }
                    break;
                }
//                初始化加载JSON后，向LDM发送订阅消息SubscribeMsg
                case ForMyself:{
                    Log.i(TAG, "Dispatcher handleMessage: "+msg.obj.toString());
                    while (ldmHandler == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (ldmHandler != null) {
                        Message subMsg = Message.obtain();
                        subMsg.what = NORMAL_MESSAGE;
                        subMsg.arg1 = MESSAGE_FROM_DISPATCHER;
                        subMsg.obj = subscribeMsg;
                        ldmHandler.sendMessage(subMsg);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    public class DispatcherBinder extends Binder {
        public void setUIHandler(Handler handler) {
            UIHandler = handler;
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    //Dispatcher bind LDM时，回调ServiceConnection
    private Handler ldmHandler = null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "LDM binding...");
            LDMService.LDMBinder binder = (LDMService.LDMBinder) service;
            ldmHandler = binder.getLdmHander();
            binder.setDispatcherHandler(mHandler);
            binder.setUIHanler(UIHandler);
//            hasLDMHandler = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate: Dispatcher");
        dispatcherThread = new HandlerThread("Dispatcher Service Thread", Process.THREAD_PRIORITY_BACKGROUND);
        dispatcherThread.start();
        mHandler = new DispatcherHandler(dispatcherThread.getLooper());
        //读取json文件，初始化订阅信息
        try {
            loadConfiguration();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (subscribeMsg != null && mHandler != null) {
            Message msg = Message.obtain();
            msg.what = ForMyself;
            msg.obj = "Configuration初始化成功，开始订阅消息！";
            mHandler.sendMessage(msg);
        }
//        mIsDestroy = false;

        super.onCreate();
//        dispatcherThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: Dispatcher");

//        mIsWork = true;
//        if (mIsDestroy) {
//            dispatcherThread.start();
//        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: UI bind Dispatcher");
        Intent intent1 = new Intent(this, LDMService.class);
        bindService(intent1, connection, BIND_AUTO_CREATE);
        Log.i(TAG, "onBind: After bind ldm");
//        mIsWork = true;
//        if (mIsDestroy) {
//            dispatcherThread.start();
//        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: UI unbind Dispatcher");
        unbindService(connection);
//        mIsWork = false;
        UIHandler = null;
        ldmHandler = null;
        return false;   // false 可能再次调用onBind()；true 可能调用onRebind()
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: Dispatcher");
//        mIsWork = false;
        dispatcherThread.quitSafely();
//        mIsDestroy = true;
        super.onDestroy();
    }

    //从JSON文件中读取订阅信息，阈值信息
    private void loadConfiguration() throws Exception{
        try {
//          读取JSON文件
//            InputStream inputStream = openFileInput("configuration.json");
           InputStream inputStream = getApplicationContext().getAssets().open("configuration.json");

            ConfigurationBean configurationBean = new GsonBuilder().create().fromJson(new InputStreamReader(inputStream), ConfigurationBean.class);

     //       output2Json(configurationBean);
            //appNum：订阅App的数量
            //加载的APP数量
            int appNum = configurationBean.getAPP().size();
        /** APPID : APPName; V2XAPP_002:TrafficSign; V2XAPP_003:APPIntersectionCollisionWarning; V2XAPP_004:APPTrafficLightOptimalSpeedAdvisory
          * APPID V2X001 为APPMe 不从JSON文件读 */
                for (int i = 0; i < appNum; i++) {
                    String appName = configurationBean.getAPP().get(i).getID();
                    String appID = configurationBean.getAPP().get(i).getID();
//                    String appID = configurationBean.getAPP().get(i).getID();
                    // 阈值
                    ConfigurationBean.APPBean.ThresholdBean threshold;
                    ArrayList<ConfigurationBean.APPBean.SubscribleMsgBean> arr;

                    switch (appName){
                        case APP_ID_APPMe:
                        {
                            //APP_ID_APPMe不用跟LDM订阅任何消息，但是LDM会返回Me信息
                            // 获取阈值
                            threshold = configurationBean.getAPP().get(i).getThreshold();
                            // 实例化应用，并利用构造函数将阈值传入
                            if (configurationBean.getAPP().get(i).getEnabled() == 1) {
                                appMe = new APPMe(threshold);
                                // 获取订阅消息
//                                    arr = configurationBean.getAPP().get(i).getSubscribleMsg();
//                                    addSubscribeMsg(appID, arr);
                            }
                        }
                            break;
                        case APP_ID_APPTrafficSign:
                        {
                            try {
                                // 获取阈值
                                threshold = configurationBean.getAPP().get(i).getThreshold();
                                // 实例化应用，并利用构造函数将阈值传入
                                if (configurationBean.getAPP().get(i).getEnabled() == 1) {
                                    appTrafficSign = new APPTrafficSign(threshold);
                                    // 获取订阅消息
//                                    arr = configurationBean.getAPP().get(i).getSubscribleMsg();
//                                    addSubscribeMsg(appID, arr);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                        case APP_ID_APPIntersectionCollisionWarning:
                        {
                            try {
                                // 获取阈值
                                threshold = configurationBean.getAPP().get(i).getThreshold();
                                // 实例化应用，并利用构造函数将阈值传入
                                if (configurationBean.getAPP().get(i).getEnabled() == 1) {
                                    appIntersectionCollisionWarning = new APPIntersectionCollisionWarning(threshold);
                                    // 获取订阅消息
                                    arr = configurationBean.getAPP().get(i).getSubscribleMsg();
                                    addSubscribeMsg(appID, arr);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                        case APP_ID_APPTrafficLightOptimalSpeedAdvisory:
                        {
                            try {

                                // 获取阈值
                                threshold = configurationBean.getAPP().get(i).getThreshold();
                                if (configurationBean.getAPP().get(i).getEnabled() == 1) {
                                    // 实例化应用，并利用构造函数将阈值传入
                                    appTrafficLightOptimalSpeedAdvisory = new APPTrafficLightOptimalSpeedAdvisory(threshold);
                                    // 获取订阅消息
                                    arr = configurationBean.getAPP().get(i).getSubscribleMsg();
                                    // 将该应用的的订阅消息传入
                                    addSubscribeMsg(appID, arr);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Exception se = new Exception(APP_EXCEPTION_DISPATCHER_LOADCONFIGURATION);
                throw se;
            }
        }

    private void addSubscribeMsg(String appID ,ArrayList<ConfigurationBean.APPBean.SubscribleMsgBean> arr) {
        for (int j = 0; j < arr.size(); j++) {
            String className = arr.get(j).getClassName();
            String filterCode = arr.get(j).getFilterCode();
            if (className != null) {
                String classID = findClassID(className);
                if (subscribeMsg.containsKey(classID)) {
                    Log.d(TAG, "loadConfiguration: "+ classID + "is exist");
                    subscribeMsg.get(classID).put(appID, filterCode);
                } else {
                    //订阅消息内部的Map，用来存放应用ID和filterCode
                    HashMap<String, String> map = new HashMap<>();
                    map.put(appID, filterCode);
                    subscribeMsg.put(classID, map);
                }
            }

        }
    }

    private String findClassID(String className) {
        switch (className) {
            case "CLASS_ID_ME":          //自车
                return "1";
            case "CLASS_ID_BSMVEHICLE":   //其他车辆:
                return "2";
            case "CLASS_ID_BSMMOTOR":    //非机动车
                return "3";
            case "CLASS_ID_BSMPEDEST":
                return "4";  //行人
            case "CLASS_ID_RSUSELF":
                return "5";    //自身RSU
            case "CLASS_ID_RSUOTHER" :     //其他RSU
                return "6";
            case "CLASS_ID_BSMNODE":
                return "7";//Node
            case "CLASS_ID_BSMLINK":
                return "8";//Link
            case "CLASS_ID_BSMLANE":
                return "9";//Lane
            case "CLASS_ID_BSMMOVEMENT" :
                return "10";//Movement
            case "CLASS_ID_BSMTRAFFICLIGHT"://TrafficLight
                return "11";
            case "CLASS_ID_BSMCHOOSEPHASE"://ChoosePhase
                return "12";
            case "CLASS_ID_BSMSIGN" :
                return "13";//Sign
            case "CLASS_ID_ME_FROM_LDM":
                return CLASS_ID_ME + "_1";
            default:
                break;
        }
        return null;
    }

    //用于第一次将外部的JSON信息写入configuration后，再将configurationBean写入android工程内
    private void output2Json(ConfigurationBean jsonBean){
        Gson gson = new Gson();
        String filename = "configuration.json";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(gson.toJson(jsonBean).getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}