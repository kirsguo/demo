package com.caeri.v2x.comm;

import java.io.IOException;

/**
 * Created by wind on 2017/4/6.
 */

public class BSMRSU {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public short getParticipantID() {
        return participantID;
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

    public byte getRSSI() {
        return RSSI;
    }

    public byte[] getBoxID() {
        return boxID;
    }

    public int getPositionTime() {
        return positionTime;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x13，表示RSU； 0x23，表示自己RSU
    private short participantID; // 交通参与者临时ID
    private double longitude; // 经度，单位为度
    private double latitude; // 纬度， 单位为度
    private byte[] NSEW = new byte[2];//“NE”表示东经北纬，“SE”表示南纬东经，“NW”表示西经北纬，“SW”表示南纬西经
    private byte RSSI;//表示无线通信信号的强度，单位-dBi
    private byte[] boxID=new byte[16];//表示盒子的ID号
    private int positionTime;//数据包中的定位信息接收的时间戳。0~59999为有效数据区，单位毫秒，表示定位时刻的秒和毫秒数值。

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[44];
        int p = 0;

        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        p = BSMFrame.short2byte(ret, p, participantID);
        p = BSMFrame.double2byte(ret, p, longitude);
        p = BSMFrame.double2byte(ret, p, latitude);
        p = BSMFrame.byte2byte(ret, p, NSEW);
        ret[p++]=RSSI;
        p = BSMFrame.byte2byte(ret, p, boxID);
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
        longitude = BSMFrame.byte2double(buf, p);
        p += Double.BYTES;
        latitude = BSMFrame.byte2double(buf, p);
        p += Double.BYTES;
        BSMFrame.byte2byte_de(buf, p, NSEW);
        p += NSEW.length;
        RSSI = buf[p++];
        BSMFrame.byte2byte_de(buf, p,boxID);
        p += boxID.length;
        positionTime = BSMFrame.byte2int(buf, p);
        p += Integer.BYTES;
        return p;
    }

    public void screen(){
        System.out.println("RSU:");
        System.out.println("elementlength:	" + elementLength + "	type(11代表机动车:	 "+ type +
                "	rsu:	" + participantID);
        String s4=new String(NSEW);
        System.out.println("rsu经度：	" +longitude + "	rsu纬度：	" + latitude + "	NSEW: 	" + s4);
        s4=new String(boxID);
        System.out.println("RSSI:	" + RSSI + "	boxID:	"+ s4 + "	positionTime:	" + positionTime);
        // TODO
        System.out.println();
    }
}
