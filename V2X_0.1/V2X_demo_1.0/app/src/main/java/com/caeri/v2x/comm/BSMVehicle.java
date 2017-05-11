package com.caeri.v2x.comm;


import java.io.IOException;


/**
 * ������������
 */

/**
 * @author �������ƻ����Ƽ����޹�˾
 *
 */

public class BSMVehicle {
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

    public byte getVehicleType1() {
        return vehicleType1;
    }

    public byte getVehicleType2() {
        return vehicleType2;
    }

    public byte getVehicleType3() {
        return vehicleType3;
    }

    public byte[] getCANinfo() {
        return CANinfo;
    }

    public byte getRSSI() {
        return RSSI;
    }

    public byte[] getBoxID() {
        return boxID;
    }

    public byte[] getVehiclePlate() {
        return vehiclePlate;
    }

    public int getPositionTime() {
        return positionTime;
    }

    private short elementLength; // 数据单元长度(包含本身字段)
    private byte type; // 0x10，表示机动车�?, 0x20，表示自身OBU
    private short participantID; // 交�?�参与�?�的临时ID
    private byte source; // 1代表视频�?2代表线圈�?3代表�?域网�?4代表微波�?测器，可扩展
    private byte sourceID; // 记录信息来源的ID
    private double longitude; // 单位为度
    private double latitude; // 单位为度
    private byte[] NSEW = new byte[2]; // “NE”表示东经北纬，“SE”表示南纬东经，“NW”表示西经北纬，“SW”表示南纬西�?
    private float speed; // 单位为公里每小时，Km/h
    private float speAngle; // 单位为角�?
    private float acceleration; // 单位为米每二次方�?,m/s2
    private float accAngle; // 单位为角�?
    private byte vehicleType1; // 1表示大中型客车，2表示牵引车，3表示城市公交车，4表示大中型货车，5表示小型汽车

    private byte vehicleType2; // 0代表普�?�车辆，�?0代表特种车辆，特种车辆类型可以扩展，如救护车、校车等。�?�需要避让车�?10-19，�?�危险车辆�??20-29
    private byte vehicleType3; // 1代表非营运车辆，2代表营运车辆
    private byte[] CANinfo = new byte[8]; // 包含左转、右转�?�刹车等CAN总线信息
    private byte RSSI; // 表示无线通信信号的强度，单位-dBi
    private byte[] boxID = new byte[16]; // 表示盒子的ID�?
    private byte[] vehiclePlate = new byte[16]; // 电子车牌�?
    private int positionTime;// 数据包中的定位信息接收的时间戳�??0~59999为有效数据区，单位毫秒，表示定位时刻的秒和毫秒数值�??

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[51];
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
        ret[p++] = vehicleType1;
        ret[p++] = vehicleType2;
        ret[p++] = vehicleType3;
        p = BSMFrame.byte2byte(ret, p, CANinfo);
        ret[p++] = RSSI;
        p = BSMFrame.byte2byte(ret, p, boxID);
        p = BSMFrame.byte2byte(ret, p, vehiclePlate);
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
        vehicleType1 = buf[p++];
        vehicleType2 = buf[p++];
        vehicleType3 = buf[p++];
        BSMFrame.byte2byte_de(buf, p, CANinfo);
        p += CANinfo.length;
        RSSI = buf[p++];
        BSMFrame.byte2byte_de(buf, p, boxID);
        p += boxID.length;
        BSMFrame.byte2byte_de(buf, p, vehiclePlate);
        p += vehiclePlate.length;
        positionTime = BSMFrame.byte2int(buf, p);
        p += Integer.BYTES;
        return p;
    }

    public void screen(){

            System.out.println("机动车");
            System.out.println("elementlength:	"+elementLength+"	type:	"+type);
            System.out.println("participantID: 	" + participantID + "	source	" + source
                    + " 	sourceID	 " + sourceID);

            String s1 = new String(NSEW);
            System.out.println("经度	" + longitude + "	纬度	" + latitude +"		NSEW:	" + s1);
            System.out.println("speed:	" + speed + "	speAngle:	" + speAngle
                    + "		acceleration:	" + acceleration + "	accAngle	" + accAngle);

            System.out.println("车辆基本类型 " + vehicleType1 + "		车辆特殊类型 :	" + vehicleType2
                    + "	车辆运营类型	" + vehicleType3);
            String ss=new String(CANinfo);
            System.out.println("CANInf:		" + ss + "	RSSI:	" + RSSI);
            ss = new String(boxID);
            String ss1=new String(vehiclePlate);
            System.out.println("boxID	" + ss + "  vehiclePlate:"+ss1+"	participantID:	" + participantID);
            System.out.println();

            // TODO:

    }
}
