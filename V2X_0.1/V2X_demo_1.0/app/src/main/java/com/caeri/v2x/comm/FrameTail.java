package com.caeri.v2x.comm;

/**
 * Title: FrameHdr
 *  @description 所有CWave帧尾定义
 *  @copyright CopyRight(C) 2017-
 *  @author 北京星云互联科技有限公司
 *  @version 0.1
 *  @date 2017-2-23
 */

import java.io.IOException;

class FrameTail {
    /**
     *
     */
    private byte chkSum;
    final byte[] magictail = { (byte) 0xea, (byte) 0xeb, (byte) 0xec,
            (byte) 0xed };

    public byte[] serialize() throws IOException {
        byte ret[] = new byte[5];
        ret[0] = chkSum;
        System.arraycopy(magictail, 0, ret, 1, magictail.length);
        return ret;
    }

    public int deSerialize(byte[] buf, int pos) throws IOException {
        int p = pos;

        chkSum = buf[p++];
        System.arraycopy(buf, p, magictail, 0, magictail.length);
        p += magictail.length;
        return p;
    }

    public byte getChkSum() {
        return chkSum;
    }

    public void setChkSum(byte n) {
        chkSum = n;
    }
}
