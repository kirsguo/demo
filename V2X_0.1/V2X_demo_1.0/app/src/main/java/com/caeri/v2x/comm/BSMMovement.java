package com.caeri.v2x.comm;

import java.io.IOException;
/**
 * Created by wind on 2017/4/6.
 */
public class BSMMovement {

    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public byte getFromNodeLocalID() {
        return fromNodeLocalID;
    }

    public byte getCenterNodeLocalID() {
        return centerNodeLocalID;
    }

    public byte getToNodeLocalID() {
        return toNodeLocalID;
    }

    public short getAllowLanes() {
        return allowLanes;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x04表示Movement
    private byte fromNodeLocalID;//上游路段起点Node的局部ID
    private byte centerNodeLocalID;//前后路段连接路口Node的局部ID
    private byte toNodeLocalID;//下游路段终点Node的局部ID
    private short allowLanes;//"16位字段，从高位到低位，分别表示从内向外的车道。最多支持单向16车道。位值为1表示该车道允许本数据单元定义的转向，0表示禁止。"


    public byte[] serialize() throws IOException {
        byte[] ret = new byte[elementLength];
        int p = 0;
        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        ret[p++] = fromNodeLocalID;
        ret[p++] = centerNodeLocalID;
        ret[p++] = toNodeLocalID;
        p = BSMFrame.short2byte(ret, p, allowLanes);

        return ret;
    }
    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];
        fromNodeLocalID = buf[p++];
        centerNodeLocalID = buf[p++];
        toNodeLocalID = buf[p++];
        allowLanes = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;

        return p;
    }
    public void screen(){
        // TODO
        System.out.println("movement");
        System.out.println("elementlength: 	" + elementLength + "	type: 	" + type);
        System.out.println("fromID: 	" + fromNodeLocalID + "	centerID: 	" + centerNodeLocalID
                + "toID " + toNodeLocalID + "	allowlanes: 	" + allowLanes);
        System.out.println();
    }
}
