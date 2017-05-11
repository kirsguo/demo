package com.caeri.v2x.util;

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



    public static final String SOURCE_DEFAULT = "0";    // 默认来源
    public static final String SOURCE_VIDEO = "1";  // 视频
    public static final String SOURCE_COIL = "2";   // 线圈
    public static final String SOURCE_LOCAL = "3";  // 本地
    public static final String SOURCE_WAVE = "4";   // 微波

    public static final String COMM_EXCEPTION_READ_FILE = "10";//打开文件失败
    public static final String COMM_EXCEPTION_CREATE_SOCKET_FAILED = "11";//套接字创建失败
    public static final String COMM_EXCEPTION_BOX_DISCONNECT = "12";//下位机失联

    public static final String APP_EXCEPTION_DISPATCHER_LOADCONFIGURATION = "30"; // Json文件加载失败



    /**
     *  应用ID
     */
    public static final String APP_ID_APPMe = "V2XAPP_001";
    public static final String APP_ID_APPTrafficSign = "V2XAPP_002";
    public static final String APP_ID_APPIntersectionCollisionWarning = "V2XAPP_003";
    public static final String APP_ID_APPTrafficLightOptimalSpeedAdvisory = "V2XAPP_004";


    public static final double EPSILON_DISTANCE = 1.5; // 距离误差，单位：m
    public static final double EPSILON_ANGLE = 5;   // 角度误差，单位：度
    public static final String MATCH_FRONT = "0001";            // 前
    public static final String MATCH_BACK = "0010";             // 后
    public static final String MATCH_LEFT = "0100";             // 左
    public static final String MATCH_RIGHT = "1000";            // 右
    public static final String MATCH_FRONT_OR_BACK = "0011";    // 前或后
    public static final String MATCH_LEFT_AND_FRONT = "0101";   // 左与前
    public static final String MATCH_LEFT_AND_BACK = "0110";    // 左与后
    public static final String MATCH_LEFT_OR_FRONT = "0111";    // 左或前
    public static final String MATCH_RIGHT_AND_FRONT = "1001";  // 右与前
    public static final String MATCH_RIGHT_AND_BACK = "1010";   // 右与后
    public static final String MATCH_RIGHT_OR_BACK = "1011";    // 右或后
    public static final String MATCH_LEFT_OR_RIGHT = "1100";    // 左或右
    public static final String MATCH_RIGHT_OR_FRONT = "1101";   // 右或前
    public static final String MATCH_LEFT_OR_BACK = "1110";     // 左或后
    public static final String MATCH_AROUND = "1111";           // 前后左右全部订阅
    public static final String MATCH_ANY = "0000";      // 不订阅（即不筛选）
    public static final String MATCH_ONLY_WITH_ME = "01";       // 只与me相同的
    public static final String MATCH_ONLY_NOT_WITH_ME = "10";   // 只与me不同的
    public static final String MATCH_ANY_WITH_ME = "00";                // 不订阅（即不筛选）
    public static final String MATCH_NOT_DEFINED_WITH_ME = "11";    // 未定义
    /***
     * UI间隔
     */
    public  static  final  int UI_INTERVAL_TrafficSign = 2;
    public  static  final  int UI_INTERVAL_IntersectionCollisionWarning = 2;
    public  static  final  int UI_INTERVAL_TrafficLightOptimalSpeedAdvisory = 1;


}
