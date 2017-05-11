package com.caeri.v2x.comm;

import java.io.IOException;
/**
 * Created by wind on 2017/4/6.
 */
public class BSMSign {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public long getFromNodeID() {
        return fromNodeID;
    }

    public long getToNodeID() {
        return toNodeID;
    }

    public byte getSignType() {
        return signType;
    }

    public double getPosLon() {
        return posLon;
    }

    public double getPosLat() {
        return posLat;
    }

    public byte[] getNSEW() {
        return NSEW;
    }

    public byte getRadius() {
        return radius;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x07表示Sign
    private long fromNodeID;//路段起点Node的全局ID
    private long toNodeID;//路段终点Node的全局ID
    private byte signType;//表示交通警告标识（【WARNING】-0x00，【SLIPPY】-0x01，【CURVE】-0x02，【ROCK】-0x03，【CONSTRUCTION】-0x04）
    private double posLon;//时间发生的经度
    private double posLat;//事件发生的纬度
    private byte[] NSEW = new byte[2];//“NE”表示东经北纬，“SE”表示南纬东经，“NW”表示西经北纬，“SW”表示南纬西经
    private byte radius;//交通事件的大概范围，单位米

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[elementLength];
        int p = 0;
        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        p = BSMFrame.long2byte(ret,p,fromNodeID);
        p = BSMFrame.long2byte(ret,p,toNodeID);
        ret[p++]=signType;
        p=BSMFrame.double2byte(ret,p,posLon);
        p=BSMFrame.double2byte(ret,p,posLat);
        p = BSMFrame.byte2byte(ret, p, NSEW);
        ret[p++]=radius;

        return ret;
    }
    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];
        fromNodeID = BSMFrame.byte2long(buf, p);
        p += Long.BYTES;
        toNodeID = BSMFrame.byte2long(buf, p);
        p += Long.BYTES;
        signType=buf[p++];
        posLon=BSMFrame.byte2double(buf,p);
        p+=Double.BYTES;
        posLat=BSMFrame.byte2double(buf,p);
        p+=Double.BYTES;
        BSMFrame.byte2byte_de(buf,p,NSEW);
        p+=NSEW.length;
        radius=buf[p++];

        return p;
    }
    public void screen()                        {
        System.out.println("sign");
        System.out.println("elementlength: 	" + elementLength + "	type: 	" + type);
        System.out.println("起点ID：	" + fromNodeID + "		终点ID：" + toNodeID + "	标牌经度：	"
                + posLon + " 	标牌纬度：	" + posLat + "		signtype(warning): 	" + signType);
        String s11=new String(NSEW);
        System.out.println("poslong: 	" + posLon +" 	poslatitude: " + posLat + " 	NSEW: 	" + s11
                + "		radius: 	" + radius);
        System.out.println();
        // TODO
    }
}
