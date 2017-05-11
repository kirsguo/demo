package com.caeri.v2x.comm;



import java.io.IOException;

/**
 * Title: BSMFrame
 *
 * @description BSM帧相关功能
 * @copyright CopyRight(C) 2017-
 * @author 北京星云互联科技有限公司
 * @version 0.1
 */

public class BSMFrame {
    // 帧数据包部分
    FrameHdr hdr = new FrameHdr();
    BSMVehicle vehicle;// 车辆数据体
    //	public BSMVehicle vehicle = new BSMVehicle();// 车辆数据体
//	public BSMMotor motor = new BSMMotor();// 非机动车数据体
    BSMMotor motor ;// 非机动车数据体
    //	public BSMPedest pedest = new BSMPedest();// 行人数据体
    BSMPedest pedest;// 行人数据体
    //	public BSMRSU rsu = new BSMRSU();// RSU数据体
    BSMRSU rsu ;// RSU数据体
    //	public BSMNode node = new BSMNode();// Node数据体
    BSMNode node;// Node数据体
    //	public BSMLink link = new BSMLink();// Link数据体
    BSMLink link;// Link数据体
    //	public BSMLane lane = new BSMLane();// Lane数据体
    BSMLane lane;// Lane数据体
    //	public BSMMovement movement = new BSMMovement();// Movement数据体
    BSMMovement movement;// Movement数据体
    //	public BSMTrafficLight trafficLight = new BSMTrafficLight();// TrafficLight数据体
    BSMTrafficLight trafficLight;// TrafficLight数据体
    //	public BSMChoosePhase choosePhase = new BSMChoosePhase();// ChoosePhase数据体
    BSMChoosePhase choosePhase;// ChoosePhase数据体
    //	public BSMSign sign = new BSMSign();// Sign数据体
    BSMSign sign;// Sign数据体


    FrameTail tail = new FrameTail();
    // 有效类型
    private int bsmType = 0; // 收到帧类型：1、机动车；2、非机动车；3、行人

    // 对外接口部分
    public BSMFrame() {
    }

    // 获得帧头类型
    public int getBSMType() {
        return bsmType;
    }

    public void setBSMType(int value) {
        bsmType = value;
    }

    public short getHdrType() {
        int n = 0;
        return byte2short(hdr.frmType, n);
    }

    /*
     * 用于序列化和反序列化的工具
     */
	/*
	 * Tools for data transmission, change target array content and move pointer
	 * to next position
	 */
    // 下面部分用于序列化字节流部分数据转换，
    public static int short2byte(byte[] target, int pos, short source) {
        int p = pos;
        target[p++] = (byte) ((source >> 8) & 0xff);
        target[p++] = (byte) (source & 0xff);
        return p;
    }

    public static int byte2byte(byte[] target, int pos, byte[] source) {
        int p = pos;
        System.arraycopy(source, 0, target, p, source.length);
        p += source.length;
        return p;
    }

    public static int int2byte(byte[] target, int pos, int source) {
        int p = pos;
        target[p++] = (byte) ((source >> 24) & 0xff);
        target[p++] = (byte) ((source >> 16) & 0xff);
        target[p++] = (byte) ((source >> 8) & 0xff);
        target[p++] = (byte) (source & 0xff);
        return p;
    }

    public static int long2byte(byte[] target, int pos, long source) {
        int p = pos;
        target[p++] = (byte) ((source >> 56) & 0xff);
        target[p++] = (byte) ((source >> 48) & 0xff);
        target[p++] = (byte) ((source >> 40) & 0xff);
        target[p++] = (byte) ((source >> 32) & 0xff);
        target[p++] = (byte) ((source >> 24) & 0xff);
        target[p++] = (byte) ((source >> 16) & 0xff);
        target[p++] = (byte) ((source >> 8) & 0xff);
        target[p++] = (byte) (source & 0xff);
        return p;
    }

    public static int float2byte(byte[] target, int pos, float source) {
        int p = pos;
        int value = Float.floatToIntBits(source);
        p = int2byte(target, pos, value);
        return p;
    }

    public static int double2byte(byte[] target, int pos, double source) {
        int p = pos;
        long value = Double.doubleToLongBits(source);
        p = long2byte(target, p, value);
        return p;
    }
    public static int char2byte(byte[] target, int pos, char[] source  ) {
        int p=pos;
        for(int i=0;i<source.length;i++)
        {
            target[p] = (byte) ((source[i] & 0xFF00) >> 8);
            p++;
            target[p] = (byte) (source[i] & 0xFF);
            p++;
        }
        return p;
    }
    public static int PassPoint2byte(byte[] target, int pos, PassPoint[] source  )
    {

        int p=pos;
        for(int i=0;i<source.length;i++){
            p=double2byte(target,p,source[i].longitude);
            p=double2byte(target,p,source[i].latitude);
            p=byte2byte(target,p,source[i].NSEW);
        }
        return p;
    }
    public static int Phase2byte(byte[] target, int pos, Phase[] source  )
    {

        int p=pos;
        for(int i=0;i<source.length;i++){
            target[p++]=source[i].phaseID;
            p=short2byte(target,p,source[i].green);
            p=short2byte(target,p,source[i].yellow);
            p=short2byte(target,p,source[i].red);
            target[p++]=source[i].status;
            p=short2byte(target,p,source[i].timeLeft);
        }
        return p;
    }
    public static int MovePhase2byte(byte[] target, int pos, MovePhase[] source  )
    {

        int p=pos;
        for(int i=0;i<source.length;i++){
            target[p++]=source[i].fromNodeLocalID;
            target[p++]=source[i].toNodeLocalID;
            target[p++]=source[i].phaseID;

        }
        return p;
    }


    // 下面部分用于反序列化字节流到各种格式
    public static void byte2byte_de(byte[] src, int p, byte[] dst) {
        System.arraycopy(src, p, dst, 0, dst.length);
    }

    public static short byte2short(byte[] src, int p) {
        short ret;
        ret = (short) ((src[p++] & 0xff) << 8);
        ret |= (short) (src[p++] & 0xff);
        return ret;
    }

    public static int byte2int(byte[] src, int p) {
        int ret;
        ret = (src[p++] & 0xff) << 24;
        ret |= (src[p++] & 0xff) << 16;
        ret |= (src[p++] & 0xff) << 8;
        ret |= (src[p++] & 0xff);
        return ret;
    }


    public static long byte2long(byte[] src, int p) {
        long ret = 0;
        ret = ((long) (src[p++] & 0xff)) << 56;
        ret |= ((long) (src[p++] & 0xff)) << 48;
        ret |= ((long) (src[p++] & 0xff)) << 40;
        ret |= ((long) (src[p++] & 0xff)) << 32;
        ret |= ((long) (src[p++] & 0xff)) << 24;
        ret |= ((long) (src[p++] & 0xff)) << 16;
        ret |= ((long) (src[p++] & 0xff)) << 8;
        ret |= (src[p++] & 0xff);
        return ret;
    }
    public static void byte2char(byte[] src, int p, char[] des) {

        int pos=p;
        for(int i=0;i<des.length;++i,pos+=2)
        {
            des[i]= (char) (((src[pos] & 0xFF) << 8) | (src[pos+1] & 0xFF));

        }

    }

    public static float byte2float(byte[] src, int p) {
        int value = byte2int(src, p);
        return Float.intBitsToFloat(value);
    }

    public static double byte2double(byte[] src, int p) {
        long value = byte2long(src, p);
        return Double.longBitsToDouble(value);
    }
    public static void byte2MovePhase(byte[] src, int p,MovePhase[] des) {
        int pos=p;
        for(int i=0;i<des.length;i++){
            des[i]=new MovePhase();
            des[i].fromNodeLocalID=src[pos++];
            des[i].toNodeLocalID=src[pos++];
            des[i].phaseID=src[pos++];

        }
    }

    public static void byte2Phase( byte[] src, int p, Phase[] des) {
        int pos=p;
        for(int i=0;i<des.length;i++){
            des[i]=new Phase();
            des[i].phaseID=src[pos++];
            des[i].green=BSMFrame.byte2short(src,pos);
            pos+=Short.BYTES;
            des[i].yellow=BSMFrame.byte2short(src,pos);
            pos+=Short.BYTES;
            des[i].red=BSMFrame.byte2short(src,pos);
            pos+=Short.BYTES;
            des[i].status=src[pos++];
            des[i].timeLeft=BSMFrame.byte2short(src,pos);
            pos+=Short.BYTES;
        }
    }

    public static void byte2PassPoint(byte[] src, int p,PassPoint[] des) {
        int pos=p;
        for(int i=0;i<des.length;i++){
            des[i]=new PassPoint();
            des[i].longitude=BSMFrame.byte2double(src,pos);
            pos+=Double.BYTES;
            des[i].latitude=BSMFrame.byte2double(src,pos);
            pos+=Double.BYTES;
            BSMFrame.byte2byte_de(src,pos,des[i].NSEW);
            pos+=des[i].NSEW.length;

        }
    }
    // 计算校验和
    public static byte calcChkSum(byte[] buf) throws IOException {
        byte ret;
        int i;

        ret = 0;
        // 数据帧从帧类型开始，越过开始和版本共八个字节
        for (i = 8; i < buf.length; i++)
            ret ^= buf[i];
        return ret;
    }

}
