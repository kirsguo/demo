package com.caeri.v2x.comm;

import java.io.IOException;
/**
 * Created by wind on 2017/4/6.
 */
public class BSMLane {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public byte getFromNodeLocalID() {
        return fromNodeLocalID;
    }

    public byte getToNodeLocalID() {
        return toNodeLocalID;
    }

    public byte getIndex() {
        return index;
    }

    public float getWidth() {
        return width;
    }

    public byte getSpeedLimit() {
        return speedLimit;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x03表示Lane
    private byte fromNodeLocalID;//起点Node的局部ID
    private byte toNodeLocalID;//终点Node的局部ID
    private byte index;//车道编号，最左侧车道编号为1，向右递增
    private float width;//车道宽度，单位，米
    private byte speedLimit;//车道限速，单位km/h

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[elementLength];
        int p = 0;
        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        ret[p++] = fromNodeLocalID;
        ret[p++] = toNodeLocalID;
        ret[p++] = index;
        p=BSMFrame.float2byte(ret,p,width);
        ret[p++]=speedLimit;

        return ret;
    }
    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];

        fromNodeLocalID = buf[p++];
        toNodeLocalID = buf[p++];
        index = buf[p++];
        width=BSMFrame.byte2float(buf,p);
        p+=Float.BYTES;
        speedLimit=buf[p++];
        return p;
    }

    public void screen()                        {
        System.out.println("lane");
        System.out.println("elementlength: 	" + elementLength + "type: 	" + type);
        System.out.println("fromnodelocalID: 	"+ fromNodeLocalID + " 	tonodelocalID: 	"+ toNodeLocalID
                + "index: " + index +" width: 	" + width + "	 speedlimit	 " + speedLimit);
        // TODO
        System.out.println();
    }
}
