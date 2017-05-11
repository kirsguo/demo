package com.caeri.v2x.comm;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.caeri.v2x.util.ConfigurationBean;
import com.caeri.v2x.util.Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by wind on 2017/4/21.
 */

public class COMMEntrance extends Thread{

    private static final String TAG = "COMMentrance";
    static CWaveApi cw; // CWaveApi 必须要有一个实例提供recvFrame()方法
    static BSMFrame bsmFrame; // 基本安全消息帧，里面可能有车、非机动车、行人，根据getBSMType()决定
    static boolean stop = false;
    static byte[] buf; // 接收缓冲区，定义一个足够大的即可，反复使用
//    static BSMVehicle vehicle; // 机动车信息
//    static BSMMotor motor; // 非机动车信息
//    static BSMPedest pedest; // 行人信息
//    static BSMRSU rsu; // RSU信息
//    static BSMNode node; // Node信息
//    static BSMLink link; // Link信息
//    static BSMLane lane; // lane信息
//    static BSMMovement movement; // Movement信息
//    static BSMTrafficLight trafficLight; // TrafficLight信息
//    static BSMChoosePhase choosephase; // ChoosePhase信息
//    static BSMSign sign; // Sign信息

    static Context context;
    final static String confName=new String("configuration.json");
    StringBuffer targetip=new StringBuffer("192.168.10.224");
    //    String targetip;
    final static StringBuffer targetport=new StringBuffer("6518");
    static StringBuffer myport=new StringBuffer("8888");
    static StringBuffer appname=new StringBuffer("V2XAPP");
    static StringBuffer boxtype=new StringBuffer("1");
    final static short  cycle=60;//注册包周期
    static Handler LDMhandler;
    static Handler UIhandler;
    COMMEntrance(Context con,Handler ldmhandler,Handler uihandler){
        context=con;
        LDMhandler=ldmhandler;
        UIhandler=uihandler;
    }
    @Override

    public void run() {
        try {
            ConfigurationBean.COMMBean.loadConfigToCOMM(context,confName,targetip,targetport,myport,appname,boxtype);
            Log.i("a",targetip+" "+targetport+" "+myport+" "+appname+" "+boxtype);
            cw = new CWaveApi(new String(appname), new String(targetip), new String(targetport),new String(myport));
            Date inidate = new Date();
            Date curdate;
            while (!stop) {
                curdate = new Date();
                if ((curdate.getTime() - inidate.getTime()) > cycle * 1000) {
                    //cw = new CWaveApi("MyApps", "192.168.10.224");

                    try {
                        cw.oneRegister();
                    }
                    catch(Exception e)
                    {
                        Exception se=new Exception(Constants.COMM_EXCEPTION_CREATE_SOCKET_FAILED);
                        throw se;
                    }

                    inidate = curdate;
                }

                try{
                    buf = cw.recvFrame();
                }
                catch (Exception e) {
                    Exception se=new Exception(Constants.COMM_EXCEPTION_BOX_DISCONNECT);
                    throw se;
                }
//                将数据结果发送到LDMhandler上
                Message mes=new Message();
//                Log.d(TAG, "run: 11111111111");

                if(cw.getBSMType()!=0) {//不是注册帧结
                    mes.what=0;
                    mes.arg1=1;
                    HashMap<String,HashMap<String,ArrayList<Object>>> result=new HashMap<String,HashMap<String,ArrayList<Object>>>();
                    String type;
                    switch(cw.getBSMType()){
                        case 1:type=Constants.SOURCE_VIDEO;
                            break;
                        case 2:type=Constants.SOURCE_COIL;
                            break;
                        case 3:type=Constants.SOURCE_LOCAL;
                            break;
                        case 4:type=Constants.SOURCE_WAVE;
                            break;
                        default:type="0";
                            break;
                    }
                    result.put(type,cw.map);
                    mes.obj=result;
                    LDMhandler.sendMessage(mes);
                }
//                curdate=new Date();
//                System.out.println("解析完毕的时间戳："+curdate.getTime());//打印当前时间戳
            }
        } catch (Exception e) {
            //e.printStackTrace();
            //IO错误，配置文件读写错误
            Message warning=new Message();
            warning.what=1;
            warning.arg1=1;
            switch(e.getMessage())
            {
                case Constants.COMM_EXCEPTION_READ_FILE://文件读写错误
                    warning.arg2=10;
                    break;
                case Constants.COMM_EXCEPTION_CREATE_SOCKET_FAILED://套接字错误
                    warning.arg2=11;
                    break;
                case Constants.COMM_EXCEPTION_BOX_DISCONNECT://下位机失联
                    warning.arg2=12;
                    break;
                default:warning.arg2=19;//默认其他错误
            }
            UIhandler.sendMessage(warning);

        } finally {
        cw.exit();
        }
    }
}
