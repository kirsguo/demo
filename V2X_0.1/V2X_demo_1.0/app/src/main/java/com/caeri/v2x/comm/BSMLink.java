package com.caeri.v2x.comm;

/**
 * Created by wind on 2017/4/21.
 */

import java.io.IOException;
/**
 * Created by wind on 2017/4/6.
 */
public class BSMLink {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public byte getNameLength() {
        return nameLength;
    }

    public byte[] getName() {
        return name;
    }

    public byte getFromNodeLocalID() {
        return fromNodeLocalID;
    }

    public byte getToNodeLocalID() {
        return toNodeLocalID;
    }

    public double getStopLongitude() {
        return stopLongitude;
    }

    public double getStopLatitude() {
        return stopLatitude;
    }

    public byte[] getNSEW() {
        return NSEW;
    }

    public byte getSpeedLimitTop() {
        return speedLimitTop;
    }

    public byte getSpeedLimitBot() {
        return speedLimitBot;
    }

    public byte getLaneNum() {
        return laneNum;
    }

    public byte getPassPointNum() {
        return passPointNum;
    }

    public byte getPassPointLength() {
        return passPointLength;
    }

    public PassPoint[] getPassPoint() {
        return passPoint;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x02表示Link
    private byte nameLength;//Link的名字长度
    private byte[] name;//Link的名字
    private byte fromNodeLocalID;//起点Node的局部ID
    private byte toNodeLocalID;//终点Node的局部ID
    private double stopLongitude;//停止线经度
    private double stopLatitude;//停止线纬度
    private byte[] NSEW = new byte[2];//“NE”表示东经北纬，“SE”表示南纬东经，“NW”表示西经北纬，“SW”表示南纬西经
    private byte speedLimitTop;//路段的最高限速
    private byte speedLimitBot;//路段的最低限速
    private byte laneNum;//路段车道数
    private byte passPointNum;//经过的点的个数
    private byte passPointLength ;//经过点的字段长度
    private PassPoint[] passPoint;


    public byte[] serialize() throws IOException {
        byte[] ret = new byte[elementLength];
        int p = 0;
        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        ret[p++] = nameLength;
        p = BSMFrame.byte2byte(ret, p, name);
        ret[p++] = fromNodeLocalID;
        ret[p++] = toNodeLocalID;
        p = BSMFrame.double2byte(ret, p, stopLongitude);
        p = BSMFrame.double2byte(ret, p, stopLatitude);
        p = BSMFrame.byte2byte(ret, p, NSEW);
        ret[p++] = speedLimitTop;
        ret[p++] = speedLimitBot;
        ret[p++] = laneNum;
        ret[p++] = passPointNum;
        ret[p++] = passPointLength;
        p=BSMFrame.PassPoint2byte(ret,p,passPoint);


        return ret;
    }

    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];
        nameLength = buf[p++];
        name=new byte[nameLength];
        BSMFrame.byte2byte_de(buf,p,name);
        p+= name.length;
        fromNodeLocalID = buf[p++];
        toNodeLocalID = buf[p++];
        stopLongitude=BSMFrame.byte2double(buf,p);
        p+=Double.BYTES;
        stopLatitude=BSMFrame.byte2double(buf,p);
        p+=Double.BYTES;
        BSMFrame.byte2byte_de(buf, p, NSEW);
        p += NSEW.length;
        speedLimitTop=buf[p++];
        speedLimitBot=buf[p++];
        laneNum=buf[p++];
        passPointNum=buf[p++];
        passPointLength=buf[p++];
        passPoint = new PassPoint[passPointNum];
        BSMFrame.byte2PassPoint(buf,p,passPoint);
        p+=passPointLength*passPointNum;
        return p;
    }
    public void screen(){
        System.out.println("Link");
        System.out.println("elementlength:	" + elementLength + "	type: 	" + type);
        String s6 = new String(name);
        System.out.println("namelength:		" + nameLength + " 	Linkname: 	" + s6);
        s6=new String(NSEW);
        System.out.println("起点ID: 	" + fromNodeLocalID + " 	终点ID: 	" + toNodeLocalID +
                " 	停止线经度：" + stopLongitude + " 	停止线纬度： " + stopLatitude + "	NSEW: 	" + s6);
        System.out.println("最高限速: 	" + speedLimitTop + " 	最低限速: 	" + speedLimitBot
                + " 	lanenum: 	" + laneNum + "	中间点数：	" + passPointNum);
        for (int i = 0; i < passPointNum; i++) {
            s6=new String(passPoint[i].NSEW);
            System.out.println("	中间点经度：	" + passPoint[i].longitude + " 	中间点纬度： " +
                    passPoint[i].latitude + " 	NSEW: 	" + s6);
        }
        System.out.println();
    }
}