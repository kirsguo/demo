package com.caeri.v2x.comm;


import java.io.IOException;

class RegisterFrame {
    /**
     * 注册帧构造类， 用于向NL多模式通讯平台发注册请求
     *
     * @param port
     *            是非常重要的变量，表示你接收UDP数据包听的端口
     * @param name
     *            是自己应用的名称，为了方便管理，最多16个字节
     * @return 无
     * @see serialize
     */

    private final FrameHdr hdr = new FrameHdr((byte) 0, (byte) 2);// 帧头

    class RegBody {
        short length;// 数据单元长度
        final byte type = 1;// 0x01表示请求
        short port;// 注册在该平台上的应用所监听的端口号
        byte[] localid = new byte[16];// 注册该平台上的应用ID，通常用应用名称

        public void setLength(short n) {
            length = n;
        }

        public byte[] serialize() throws IOException {
            byte ret[];
            int p = 0;
            ret = new byte[2 + 1 + 2 + 16];
            p = BSMFrame.short2byte(ret, p, length);
            ret[p] = type;
            p++;
            p = BSMFrame.short2byte(ret, p, port);
            p = BSMFrame.byte2byte(ret, p, localid);
            return ret;
        }
    };

    private RegBody body = new RegBody();// 注册帧帧体
    private final FrameTail tail = new FrameTail();// 帧尾

    // 以上是数据区

    public RegisterFrame(short port_in, String name) {

        byte[] b = name.getBytes();
        body.port = port_in;
        System.arraycopy(b, 0, body.localid, 0, b.length);
    }

	/*
	 * private byte calcChkSum(byte[] buf) throws IOException { byte ret; int i;
	 *
	 * ret = 0; // 数据帧从帧类型开始，越过开始和版本共八个字节 for (i = 8; i < buf.length; i++) ret
	 * ^= buf[i]; return ret; }
	 */

    public byte[] serialize() throws IOException {
        /**
         * 注册帧RegisterFrame的serialize方法
         * <p>
         * 该方法在CWaveApi向平台发送注册请求时被调用，生成一个注册帧的二进制流
         *
         * @version 0.1
         * @param 无
         * @return byte[] 发送注册帧所需要的字节流数据
         * @see RegisterFrame
         */

        byte ret[];
        int hdrlen, bodylen, taillen;
        // 得到三个部分的字节长度
        hdrlen = hdr.serialize().length;
        bodylen = body.serialize().length;
        taillen = tail.serialize().length;
        // 初始化字节流，也就是注册帧缓冲区
        ret = new byte[hdrlen + bodylen + taillen];

        // 将长度填写到包体，包头和包体部分都需要填写，这里两个长度是一样的。
        hdr.setDataLen((short) bodylen);
        body.setLength((short) bodylen);
        System.arraycopy(hdr.serialize(), 0, ret, 0, hdrlen);
        System.arraycopy(body.serialize(), 0, ret, hdrlen, bodylen);
        // 计算数据表部分校验和，填入包尾部
        tail.setChkSum(BSMFrame.calcChkSum(ret));
        System.arraycopy(tail.serialize(), 0, ret, hdrlen + bodylen, taillen);

        return ret;
    }
}
