package com.caeri.v2x.comm;

/**
 * Title: FrameHdr
 *  @description 所有CWave帧头定义
 *  @copyright CopyRight(C) 2017-
 *  @author 北京星云互联科技有限公司
 *  @version 0.1
 *  @date 2017-2-23
 */

import java.io.IOException;

class FrameHdr {
    /**
     *
     */
    // frame head part
    final byte[] magic = new byte[] { (byte) 0xfa, (byte) 0xfb, (byte) 0xfc,
            (byte) 0xfd };
    short currentVer = 0;
    short minVer = 0;
    byte[] frmType = new byte[2];
    byte[] dstAddr = new byte[16];
    byte[] srcAddr = new byte[16];
    byte[] dstAppID = new byte[16];
    byte[] srcAppID = new byte[16];
    short dataLen;

    public void setDataLen(short l) {
        dataLen = l;
    }

    public FrameHdr() {

    }

    public FrameHdr(byte t1, byte t2) {
        frmType[0] = t1;
        frmType[1] = t2;
    }

    public byte[] serialize() throws IOException {
        byte[] ret = new byte[76];
        int p = 0;

        p = BSMFrame.byte2byte(ret, p, magic);
        p = BSMFrame.short2byte(ret, p, currentVer);
        p = BSMFrame.short2byte(ret, p, minVer);
        p = BSMFrame.byte2byte(ret, p, frmType);
        p = BSMFrame.byte2byte(ret, p, dstAddr);
        p = BSMFrame.byte2byte(ret, p, srcAddr);
        p = BSMFrame.byte2byte(ret, p, dstAppID);
        p = BSMFrame.byte2byte(ret, p, srcAppID);
        p = BSMFrame.short2byte(ret, p, dataLen);
        return ret;
    }

    public int deSerialize(byte[] buf, int pos) throws IOException {
        int p = pos;

        BSMFrame.byte2byte_de(buf, p, magic);
        p += magic.length;
        currentVer = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        minVer = BSMFrame.byte2short(buf, p);
        p += Short.BYTES;
        BSMFrame.byte2byte_de(buf, p, frmType);
        p += frmType.length;
        BSMFrame.byte2byte_de(buf, p, dstAddr);
        p += dstAddr.length;
        BSMFrame.byte2byte_de(buf, p, srcAddr);
        p += srcAddr.length;
        BSMFrame.byte2byte_de(buf, p, dstAppID);
        p += dstAppID.length;
        BSMFrame.byte2byte_de(buf, p, srcAppID);
        p += srcAppID.length;
        dataLen = BSMFrame.byte2short(buf, p);
        p += 2;
        return p;
    }
}
