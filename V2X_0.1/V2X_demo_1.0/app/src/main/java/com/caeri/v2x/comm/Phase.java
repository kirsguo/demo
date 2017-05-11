package com.caeri.v2x.comm;


public class Phase {
    public byte getPhaseID() {
        return phaseID;
    }

    public short getGreen() {
        return green;
    }

    public short getYellow() {
        return yellow;
    }

    public short getRed() {
        return red;
    }

    public byte getStatus() {
        return status;
    }

    public short getTimeLeft() {
        return timeLeft;
    }

    byte phaseID;//该相位的ID
    short green;//绿灯时间
    short yellow;//黄灯时间
    short red;//红灯时间
    byte status;//当前状态：1-绿灯、2-黄灯、3-红灯、4-红闪、5-绿闪、6-黄闪、7-灯全灭
    short timeLeft;//当前剩余时间
}
