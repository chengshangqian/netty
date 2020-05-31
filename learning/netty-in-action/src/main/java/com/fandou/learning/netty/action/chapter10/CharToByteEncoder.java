package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 字符-字节编码器
 */
public class CharToByteEncoder extends MessageToByteEncoder<Character> {
    /**
     * 将字符编码为字节写入到出站的ByteBuf中
     *
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Character msg, ByteBuf out) throws Exception {
        // 将字符编码为字节写入到出站的ByteBuf中
        System.out.println("出站数据msg => " + msg);
        out.writeChar(msg);
    }
}
