package com.example.kirsguo.demo_2.util;

/**
 * Created on 4/26/026.
 *
 * @author Benjamin
 */

public final class Constants {
    public static final int NORMAL_MESSAGE = 0;         // 普通的消息，带有正常情况下需要传递的数据。用于message.what。
    public static final int DEFAULT_MESSAGE = 0;        // 缺省情况。用于message.arg1。
    public static final int MESSAGE_FROM_COMM = 1;      // 来自COMM的消息。用于message.arg1
    public static final int MESSAGE_FROM_DISPATCHER = 2;// 来自Dispatcher的消息。用于message.arg1。
    public static final int ERROR_MESSAGE = 1;          // 错误的消息，带有出错情况下需要传递的数据。用于message.what。
    public static final int DEFAULT_ERROR = 0;          // 缺省情况。用于message.arg1。
    public static final int ERROR_FROM_COMM = 1;        // 来自COMM的错误信息。用于message.arg1。
    public static final int ERROR_FROM_LDM = 2;         // 来自LDM的错误信息。用于message.arg1。
    public static final int ERROR_FROM_DISPATCHER = 3;  // 来自Dispatcher的错误信息。用于message.arg1。

    public static final String CLASS_ID_ME = "1";           //自车
    public static final String CLASS_ID_BSMVEHICLE = "2";   //其他车辆
    public static final String CLASS_ID_BSMMOTOR = "3";     //非机动车
    public static final String CLASS_ID_BSMPEDEST = "4";    //行人
    public static final String CLASS_ID_RSUSELF = "5";      //自身RSU
    public static final String CLASS_ID_RSUOTHER = "6";     //其他RSU
    public static final String CLASS_ID_BSMNODE = "7";      //Node
    public static final String CLASS_ID_BSMLINK = "8";      //Link
    public static final String CLASS_ID_BSMLANE = "9";      //Lane
    public static final String CLASS_ID_BSMMOVEMENT = "10";     //Movement
    public static final String CLASS_ID_BSMTRAFFICLIGHT = "11";//TrafficLight
    public static final String CLASS_ID_BSMCHOOSEPHASE = "12";//ChoosePhase
    public static final String CLASS_ID_BSMSIGN = "13";//Sign
    public static final String CLASS_ID_ME_FROM_LDM = CLASS_ID_ME + "_1";

    public static final String SOURCE_VIDEO = "1";  // 视频
    public static final String SOURCE_COIL = "2";   // 线圈
    public static final String SOURCE_LOCAL = "3";  // 本地
    public static final String SOURCE_WAVE = "4";   // 微波



    /**
     *  应用ID
     */
    public static final String APP_ID_Me = "V2XAPP_001";
    public static final String APP_ID_TrafficSign = "V2XAPP_002";
    public static final String APP_ID_APPIntersectionCollisionWarning = "V2XAPP_003";
    public static final String APP_ID_APPTrafficLightOptimalSpeedAdvisory = "V2XAPP_004";

}
