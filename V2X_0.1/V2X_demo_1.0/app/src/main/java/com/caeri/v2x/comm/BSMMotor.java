package com.caeri.v2x.comm;

/**
 * Created by wind on 2017/4/21.
 */

import java.io.IOException;

/**
 * 非机动车数据体
 */

/**
 * @author 北京星云互联科技有限公司
 *
 */
public class BSMMotor {
    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x11表示非机动车
    private short participantID; // 交通参与者临时ID
    private byte source; // 数据来源：1、视频；2、线圈；3、局域网；4、微波...
    private byte sourceID; // 记录信息来源的ID
    private double longitude; // 经度，单位为度
    private double latitude; // 纬度， 单位为度
    private byte[] NSEW = new byte[2]; // "NE"表示东经北纬.....
    private float speed; // 速度，单位公里/小时，km/h
    private float speAngle; // 速度方向角,以正北方向顺时针，单位为角度
    private float acceleration; // 加速度，单位为米每平方秒
    private float acclAngle; // 加速度方向角，以正北方向顺时针，单位为角度
    private byte vehicleType; // 车辆基本类型：1、电动车；2、自行车；3、三轮车...；
    private int positionTime;// 定位时间戳。0-59999有效数据，单位毫秒

    public byte[] serialize() throws IOException {// 序列化，返回值为字节流
        byte[] ret = new byte[46];
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
        p = BSMFrame.float2byte(ret, p, acclAngle);
        ret[p++] = vehicleType;
        p = BSMFrame.int2byte(ret, p, positionTime);
        return ret;
    }

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

    public float getAcclAngle() {
        return acclAngle;
    }

    public byte getVehicleType() {
        return vehicleType;
    }

    public int getPositionTime() {
        return positionTime;
    }

    public int deSerialize(byte[] buf, int pos) {// 反序列化，返回值为长度
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
        acclAngle = BSMFrame.byte2float(buf, p);
        p += Float.BYTES;
        vehicleType = buf[p++];
        positionTime = BSMFrame.byte2int(buf, p);
        p += Integer.BYTES;
        return p;
    }
    public void screen(){
        System.out.println("非机动车");
        System.out.println("elementlength:	"+elementLength + "	type(11代表机动车:	"+type);
        System.out.println("participantID:	" + participantID + "		source(4代表微波检测器) 	" + source
                + "		sourceID	" + sourceID);
        String s1 = new String(NSEW);
        System.out.println("经度：	" + longitude + "		纬度：" + latitude +"		NSEW: 	" + s1);
        System.out.println("speed:		" + speed + " 	speAngle: " + speAngle
                + "		acceleration: " + acceleration + "	accAngle：	" + acclAngle);

        System.out.println("车辆基本类型：	" + vehicleType + "	positionTime:	" + positionTime);
        System.out.println();
        // TODO: 车辆信息已经得到，后续处理
    }
}
