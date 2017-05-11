package com.caeri.v2x.comm;

/**
 * 程序名称：
 *		CWaveApi
 * 功能描述：
 *		本类用java实现NL多模式通信平台服务数据接口
 * 接口函数和用法：
 *		CWaveApi(String appName, String address, short port)
 *			appName 自己定义应用的名称, 8字节长的字符串（支持汉字）
 *			address	通讯平台的IP地址，缺省为192.168.10.224，具体请向星云互联技术确认
 *			port 	本地听的UDP端口号，缺省为8000
 *		注意：构造函数如果要更换address, 必须填写appName
 *			错误用法： CWaveApi cwApi = new CWaveApi("192.168.20.224");
 *			正确用法： CWaveApi cwAPi = new CWaveApi(“MyAPP”，"192.168.20.224");
 *		BSMFrame recvFrame()
 *			阻塞函数，当有数据时，返回BSM类
 * 版本：
 *		Version 0.1
 * 日期：
 *		2017-2-22
 */
import android.util.Log;

import com.caeri.v2x.util.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class CWaveApi {
    private static final String TAG = "CWaveApi";
    final private int MAX_RCV_BUF = 4096;
    private DatagramSocket sock; // 应用接收BSMFrame听的socket
    private short myport = 8000; // 应用接收BSMFrame的UDP端口
    private String appName; // 应用的名称，8个汉字以内
    private InetAddress addr; // OBU（RSU）服务所在的IP地址
    private RegisterFrame regFrm; // 注册帧
    private short targetport=6518;

    private BSMFrame bsm = new BSMFrame(); // 接收到的帧存放在这里
    private byte[] recvBuf;
    private DatagramPacket recvPacket;
//map set get
    public HashMap<String, ArrayList<Object>> getMap() {
        return map;
    }

    public void setMap(HashMap<String, ArrayList<Object>> map) {
        this.map = map;
    }

    public HashMap<String,ArrayList<Object>> map;

    private void register() throws Exception {
        DatagramSocket sk = new DatagramSocket();
        byte[] sendbuf;
        DatagramPacket sendPacket;

        regFrm = new RegisterFrame((short) myport, appName);
        sendbuf = regFrm.serialize();
        sendPacket = new DatagramPacket(sendbuf, sendbuf.length, addr, targetport);
        sk.send(sendPacket);
        sk.close();
    }
    public void oneRegister()throws Exception{
        register();
    }
    public static void format_dump_buf(byte[] buf) {
        //if(buf[64+14]!=0x20&&buf[64+14]!=0x10&&buf[64+14]!=0x01) {
        int i, j = 0;
        String s;
        for (i = 0; i < buf.length; i++, j++) {
            s = Integer.toHexString(buf[i] & 0xff);
            if ((j & 0xf) == 0)
                System.out.println();
            if (s.length() == 1)
                s = "0" + s;
            System.out.print(s + " ");
        }
        System.out.println();
        System.out.println();
        //}
    }
    public DatagramSocket getSock(){
        return sock;
    }

    private void init() throws Exception {
        try {

            sock = new DatagramSocket(myport);

        sock.setSoTimeout(5000);
        recvBuf = new byte[MAX_RCV_BUF];
        recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
        } catch (SocketException e1) {
        e1.printStackTrace();
        }
    }


    // 解交通参与者包体
    private boolean parseBSM(byte[] buf) throws Exception {
        int p;
        p = 0;
        p = bsm.hdr.deSerialize(recvBuf, p);
        int tailp=p+bsm.hdr.dataLen;
        map=new HashMap<String, ArrayList<Object>>();
        for(int i=1;i<=13;i++){
            ArrayList<Object> temp=new ArrayList<Object>();
            String tempstring=""+i;
            map.put(tempstring,temp);
        }
        //交通参与者
        if (bsm.getHdrType() == (short) 6){
            bsm.setBSMType(1);//1代表交通参与者
            while(p<tailp) {
                // 交通参与者数据包
                switch (recvBuf[p + 2]) {
                    case 0x20:// 自车
                    case 0x10: // 机动车解包
                        bsm.vehicle=new BSMVehicle();
                        p = bsm.vehicle.deSerialize(buf, p);
                        if(bsm.vehicle.getElementLength()!=89) {
                            p += bsm.vehicle.getElementLength() - 89;//来源于微波的车辆包少4个字节
                        }

                        if(bsm.vehicle.getType()==0x10)
                            map.get(Constants.CLASS_ID_BSMVEHICLE).add(bsm.vehicle);
                        else
                            map.get(Constants.CLASS_ID_ME).add(bsm.vehicle);

                        bsm.setBSMType(bsm.vehicle.getSource());
//                        bsm.vehicle.screen();
//                        Log.d(TAG, "parseBSM: 22222222222222222");
//                        ((BSMVehicle) map.get("1").get(0)).screen();


                        break;
                    case 0x11: // 非机动车解包
                        bsm.motor= new BSMMotor();
                        p = bsm.motor.deSerialize(buf, p);
                        map.get(Constants.CLASS_ID_BSMMOTOR).add(bsm.motor);
                        bsm.setBSMType(bsm.motor.getSource());

                    break;
                    case 0x12://行人
                        bsm.pedest= new BSMPedest();
                        p = bsm.pedest.deSerialize(buf, p);
                        map.get(Constants.CLASS_ID_BSMPEDEST).add(bsm.pedest);
                        bsm.setBSMType(bsm.pedest.getSource());
                        break;

                    case 0x13://rsu
                    case 0x23://自己RSU
                        bsm.rsu= new BSMRSU();
                        p = bsm.rsu.deSerialize(buf, p);
                        if(bsm.rsu.getType()==0x13)
                            map.get(Constants.CLASS_ID_RSUSELF).add(bsm.rsu);
                        else
                            map.get(Constants.CLASS_ID_RSUOTHER).add(bsm.rsu);
                    break;
                }
            }
            p = bsm.tail.deSerialize(buf, p);
            return true;
        }
        //逻辑路网数据
        else {
            if (bsm.getHdrType() == (short) 8) {
                bsm.setBSMType(2);//2代表交通参与者
                while (p < tailp) {
                    switch (recvBuf[p + 2]) {
                        case 0x01: // Node
                            bsm.node = new BSMNode();
                            p = bsm.node.deSerialize(buf, p);
                            map.get(Constants.CLASS_ID_BSMNODE).add(bsm.node);

                        break;
                        case 0x02: // Link
                            bsm.link = new BSMLink();
                            p = bsm.link.deSerialize(buf, p);
                            map.get(Constants.CLASS_ID_BSMLINK).add(bsm.link);

                        break;
                        case 0x03: // Lane
                            bsm.lane = new BSMLane();
                            p = bsm.lane.deSerialize(buf, p);
                            map.get(Constants.CLASS_ID_BSMLANE).add(bsm.lane);

                        break;
                        case 0x04://Movement
                            bsm.movement = new BSMMovement();
                            p = bsm.movement.deSerialize(buf, p);
                            map.get(Constants.CLASS_ID_BSMMOVEMENT).add(bsm.movement);

                        break;
                        case 0x05://TrafficLight
                            bsm.trafficLight = new BSMTrafficLight();
                            p = bsm.trafficLight.deSerialize(buf, p);
                            map.get(Constants.CLASS_ID_BSMTRAFFICLIGHT).add(bsm.trafficLight);

                        break;
                        case 0x06://ChoosePhase
                            bsm.choosePhase = new BSMChoosePhase();
                            p = bsm.choosePhase.deSerialize(buf, p);
                            map.get(Constants.CLASS_ID_BSMCHOOSEPHASE).add(bsm.choosePhase);

                        break;
                        case 0x07://Sign
                            bsm.sign = new BSMSign();
                            p = bsm.sign.deSerialize(buf, p);
                            map.get(Constants.CLASS_ID_BSMSIGN).add(bsm.sign);

                        break;
                    }
                }
                bsm.setBSMType(2);
                p = bsm.tail.deSerialize(buf, p);
                return true;
            } else
                return false;
        }
    }

    // 以下为接口函数
    public void exit()  {
        sock.close();
    }

    public byte[] recvFrame() throws Exception {
        byte[] ret;
        sock.receive(recvPacket);
        Date curdate=new Date();
        System.out.println("接收的时间戳："+curdate.getTime());//打印当前时间戳
        ret = new byte[recvPacket.getLength()];
        System.arraycopy(recvBuf, 0, ret, 0, ret.length);
        bsm.setBSMType(0);
        parseBSM(ret);
        return ret;
    }

    // 获得交通参与者类型
    public int getHdrType() {
        return bsm.getHdrType();
    }

    public int getBSMType() {
        return bsm.getBSMType();
    }

    // 获得机动车数据
    public BSMVehicle getBSMVehicle() {
        return bsm.vehicle;
    }

    // 获得非机动车数据
    public BSMMotor getBSMMotor() {
        return bsm.motor;
    }

    // 获得行人数据
    public BSMPedest getBSMPedest() {
        return bsm.pedest;
    }
    //获得RSU数据
    public BSMRSU getBSMRSU() {
        return bsm.rsu;
    }
    //获得Node数据
    public BSMNode getBSMNode() {
        return bsm.node;
    }
    //获得Link数据
    public BSMLink getBSMLink() {
        return bsm.link;
    }
    //获得Lane数据
    public BSMLane getBSMLane() {
        return bsm.lane;
    }
    //获得Movement数据
    public BSMMovement getBSMMovement() {
        return bsm.movement;
    }
    //获得TrafficLight数据
    public BSMTrafficLight getBSMTrafficLight() {
        return bsm.trafficLight;
    }
    //获得ChoosePhase数据
    public BSMChoosePhase getBSMChoosePhase() {
        return bsm.choosePhase;
    }
    //获得Sign数据
    public BSMSign getBSMSign() {
        return bsm.sign;
    }


    // 以下为建构函数
    CWaveApi() {
        appName = "TestAPP";
        try {
            addr = InetAddress.getByName("192.168.10.224");
            init();
            register();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    CWaveApi(short in_port) {
//        appName = "TestAPP";
//        myport = in_port;
//        try {
//            addr = InetAddress.getByName("192.168.10.224");
//            init();
//            register();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    public CWaveApi(String s) {
//        appName = new String(s);
//        try {
//            addr = InetAddress.getByName("192.168.10.224");
//            init();
//            register();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public CWaveApi(String s, String in_addr) {
//        appName = new String(s);
//        try {
//            addr = InetAddress.getByName(in_addr);
//            init();
//            register();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public CWaveApi(String appname, String tip, String tport,String mport) throws Exception{
        appName = new String(appname);
        myport = Short.parseShort(mport);
        targetport=Short.parseShort(tport);
        Log.i("a", "this is in API" + appName+" "+myport+" " +targetport+" " + tip);
        //appName=new String("aname");
        try {
//            Log.i("a", "begin address");
            addr = InetAddress.getByName(tip);
//            Log.i("a", "address success");
            init();
//            Log.i("a", "init success");
            register();
//            Log.i("a", "register success");
        } catch (Exception e) {
//            e.printStackTrace();
            Exception se=new Exception(Constants.COMM_EXCEPTION_CREATE_SOCKET_FAILED);
            throw se;
        }
    }

//    public CWaveApi(String s, short in_port) {
//        appName = new String(s);
//        myport = in_port;
//        try {
//            addr = InetAddress.getByName("192.168.10.224");
//            init();
//            register();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}