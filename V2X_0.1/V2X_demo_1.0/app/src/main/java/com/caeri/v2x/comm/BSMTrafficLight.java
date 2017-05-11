package com.caeri.v2x.comm;

import java.io.IOException;
/**
 * Created by wind on 2017/4/7.
 */
public class BSMTrafficLight {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public byte getCenterNodeLocalID() {
        return centerNodeLocalID;
    }

    public short getCycle() {
        return cycle;
    }

    public byte getPhaseNum() {
        return phaseNum;
    }

    public byte getPhaseLength() {
        return phaseLength;
    }

    public Phase[] getPhase() {
        return phase;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x05表示TrafficLight
    private byte centerNodeLocalID;//信号灯路口Node的局部ID
    private short cycle;//信号灯周期
    private byte phaseNum;//相位个数
    private byte phaseLength;//相位字节长度
    private Phase[] phase;//相位数组

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[elementLength];
        int p = 0;
        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        ret[p++] = centerNodeLocalID;
        p = BSMFrame.short2byte(ret,p,cycle);
        ret[p++]=phaseNum;
        ret[p++]=phaseLength;
        p = BSMFrame.Phase2byte(ret,p,phase);


        return ret;
    }

    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];
        centerNodeLocalID = buf[p++];
        cycle = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        phaseNum = buf[p++];
        phaseLength = buf[p++];
        phase=new Phase[phaseNum];  // 没有构造数组对象的元素
        BSMFrame.byte2Phase(buf,p,phase);
        p+=phaseLength*phaseNum;


        return p;
    }
    public void screen()                        {
        System.out.println("trafficlight");
        System.out.println("elementlength: 	" + elementLength + "		type: " + type);
        System.out.println("信号灯路口Node的局部ID: 	" + centerNodeLocalID + "	cycle: 	"
                + cycle + "	phasenum 	" +  phaseNum + "	phaselength 	" + phaseLength);
        // TODO
        for (int i = 0; i < phaseNum; i++) {
            System.out.println("phaseId: 	" + phase[i].phaseID + "	green: 	" + phase[i].green
                    + "	yellow: 	" + phase[i].yellow + "	red 	" + phase[i].red);
            System.out.println("当前灯状态：	" + phase[i].status + " 当前剩余时间	"
                    + phase[i].timeLeft);
        }
        System.out.println();
    }
}
