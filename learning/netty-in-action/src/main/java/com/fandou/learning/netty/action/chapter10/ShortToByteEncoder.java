package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 短整型-字节编码器：指定泛型为Short类型，其它类型的数据不会被传入进来
 */
public class ShortToByteEncoder extends MessageToByteEncoder<Short> {
    /**
     * 将Short类型编码为ByteBuf输入
     *
     * @param ctx 管道处理器上下文
     * @param msg 需要出站的Short类型数据
     * @param out 编码结果类型
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Short msg, ByteBuf out) throws Exception {
        System.out.println("msg => " + msg);
        out.writeShort(msg);
    }
}
