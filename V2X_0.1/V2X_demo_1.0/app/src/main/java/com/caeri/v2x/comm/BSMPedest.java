package com.caeri.v2x.comm;

import java.io.IOException;

/**
 * 行人数据体
 */

/**
 * @author 北京星云互联科技有限公司
 *
 */
public class BSMPedest {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public short getParticipantID() {
        return participantID;
    }

    public byte getSource() {
        return source;
    }

    public byte getSourceID() {
        return sourceID;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public byte[] getNSEW() {
        return NSEW;
    }

    public float getSpeed() {
        return speed;
    }

    public float getSpeAngle() {
        return speAngle;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public float getAccAngle() {
        return accAngle;
    }

    public int getPositionTime() {
        return positionTime;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x12表示机动车
    private short participantID; // 交通参与者临时ID
    private byte source; // 数据来源：1、视频；2、线圈；3、局域网；4、微波...
    private byte sourceID; // 数据来源ID
    private double longitude; // 经度，单位为度
    private double latitude; // 纬度， 单位为度
    private byte[] NSEW = new byte[2];//“NE”表示东经北纬，“SE”表示南纬东经，“NW”表示西经北纬，“SW”表示南纬西经
    private float speed; // 速度，单位公里/小时，km/h
    private float speAngle; // 速度方向角,以正北方向顺时针，单位为角度
    private float acceleration; // 加速度，单位为米每平方秒
    private float accAngle; // 加速度方向角，以正北方向顺时针，单位为角度
    private int positionTime;//数据包中的定位信息接收的时间戳。0~59999为有效数据区，单位毫秒，表示定位时刻的秒和毫秒数值。

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[45];
        int p = 0;

        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        p = BSMFrame.short2byte(ret, p, participantID);
        ret[p++] = source;
        ret[p++] = sourceID;
        p = BSMFrame.double2byte(ret, p, longitude);
        p = BSMFrame.double2byte(ret, p, latitude);
        p = BSMFrame.byte2byte(ret, p, NSEW);
        p = BSMFrame.float2byte(ret, p, speed);
        p = BSMFrame.float2byte(ret, p, speAngle);
        p = BSMFrame.float2byte(ret, p, acceleration);
        p = BSMFrame.float2byte(ret, p, accAngle);
        p = BSMFrame.int2byte(ret, p, positionTime);
        return ret;
    }

    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];
        participantID = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        source = buf[p++];
        sourceID = buf[p++];
        longitude = BSMFrame.byte2double(buf, p);
        p += Double.BYTES;
        latitude = BSMFrame.byte2double(buf, p);
        p += Double.BYTES;
        BSMFrame.byte2byte_de(buf, p, NSEW);
        p += NSEW.length;
        speed = BSMFrame.byte2float(buf, p);
        p += Float.BYTES;
        speAngle = BSMFrame.byte2float(buf, p);
        p += Float.BYTES;
        acceleration = BSMFrame.byte2float(buf, p);
        p += Float.BYTES;
        accAngle = BSMFrame.byte2float(buf, p);
        p += Float.BYTES;
        positionTime = BSMFrame.byte2int(buf, p);
        p += Integer.BYTES;
        return p;
    }
    public void screen()
    {
        System.out.println("行人");
    }
}
