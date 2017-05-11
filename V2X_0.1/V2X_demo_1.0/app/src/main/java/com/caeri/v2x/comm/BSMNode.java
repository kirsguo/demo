package com.caeri.v2x.comm;

import java.io.IOException;

/**
 * 行人数据体
 */

/**
 * @author 北京星云互联科技有限公司
 *
 */
public class BSMNode {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public long getGlobalID() {
        return globalID;
    }

    public byte getLocalID() {
        return localID;
    }

    public byte getNameLength() {
        return nameLength;
    }

    public byte[] getName() {
        return name;
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

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x01表示Node
    private long globalID; // 节点的全局ID
    private byte localID; // 节点的局部ID
    private byte nameLength;//节点的名字长度
    private byte[] name;//节点名字
    private double longitude; // 经度，单位为度
    private double latitude; // 纬度， 单位为度
    private byte[] NSEW = new byte[2];//“NE”表示东经北纬，“SE”表示南纬东经，“NW”表示西经北纬，“SW”表示南纬西经

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[elementLength];
        int p = 0;
        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        p = BSMFrame.long2byte(ret, p, globalID);
        ret[p++] = localID;
        ret[p++] = nameLength;
        p=BSMFrame.byte2byte(ret,p,name);
        p = BSMFrame.double2byte(ret, p, longitude);
        p = BSMFrame.double2byte(ret, p, latitude);
        p = BSMFrame.byte2byte(ret, p, NSEW);

        return ret;
    }

    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];
        globalID = BSMFrame.byte2long(buf, p);
        p += Long.BYTES;
        localID = buf[p++];
        nameLength = buf[p++];
        name=new byte[nameLength];
        BSMFrame.byte2byte_de(buf,p,name);
        p+=nameLength;
        longitude = BSMFrame.byte2double(buf, p);
        p += Double.BYTES;
        latitude = BSMFrame.byte2double(buf, p);
        p += Double.BYTES;
        BSMFrame.byte2byte_de(buf, p, NSEW);
        p += NSEW.length;
        return p;
    }
    public void screen(){
        System.out.println("Node");
        System.out.println("elementlength:	" + elementLength + "	type:	" + type);
        System.out.println("glabalID:	" + globalID + "	localID:	" + localID);
        String s5=new String(name);
        System.out.println("namelength:	" + nameLength + "		name:	" + s5);
        s5 = new String(NSEW);
        System.out.println(" 经度：	" + longitude + " 	纬度： " + latitude + "	经纬度扩展:	 " + s5);
        // TODO
        System.out.println();
    }
}
