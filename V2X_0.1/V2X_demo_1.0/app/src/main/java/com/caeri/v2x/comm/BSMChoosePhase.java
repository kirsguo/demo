package com.caeri.v2x.comm;

import java.io.IOException;
/**
 * Created by wind on 2017/4/7.
 */
public class BSMChoosePhase {
    public short getElementLength() {
        return elementLength;
    }

    public byte getType() {
        return type;
    }

    public byte getCenterNodeLocalID() {
        return centerNodeLocalID;
    }

    public byte getMovePhaseNum() {
        return movePhaseNum;
    }

    public byte getMovePhaseLength() {
        return movePhaseLength;
    }

    public MovePhase[] getMovePhase() {
        return movePhase;
    }

    private short elementLength; // 数据单元长度
    private byte type; // 类型，0x06表示ChoosePhase
    private byte centerNodeLocalID;//前后路段连接路口Node的局部ID
    private byte movePhaseNum;//Movement和Phase的对应关系个数
    private byte movePhaseLength;//对应关系的字段长度
    private MovePhase[] movePhase;//转向-相位关系数组
    public byte[] serialize() throws IOException {
        byte[] ret = new byte[elementLength];
        int p = 0;
        p = BSMFrame.short2byte(ret, p, elementLength);
        ret[p++] = type;
        ret[p++] = centerNodeLocalID;
        ret[p++] = movePhaseNum;
        ret[p++] = movePhaseLength;
        p = BSMFrame.MovePhase2byte(ret,p,movePhase);
        return ret;
    }

    public int deSerialize(byte[] buf, int pos) {
        int p = pos;

        elementLength = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        type = buf[p++];
        centerNodeLocalID = buf[p++];
        movePhaseNum = buf[p++];
        movePhaseLength = buf[p++];
        movePhase=new MovePhase[movePhaseNum];
        BSMFrame.byte2MovePhase(buf,p,movePhase);
        p+=movePhaseLength*movePhaseNum;


        return p;
    }
    public void screen()                        {
        System.out.println("choosephase");
        System.out.println("elementlength: 	" + elementLength + "	type: 	" +  type);
        System.out.println("centerID: " +  centerNodeLocalID + "	movephasenum: 	" +  movePhaseNum
                + "	movephaselength: 	" +  movePhaseLength);
        for(int i=0;i< movePhaseNum;i++){
            System.out.println("fromId: 	" + movePhase[i].fromNodeLocalID
                    + "		toID: 	" +  movePhase[i].toNodeLocalID + "		phaseID 	" +  movePhase[i].phaseID);
        }
        System.out.println();
        // TODO
    }

}