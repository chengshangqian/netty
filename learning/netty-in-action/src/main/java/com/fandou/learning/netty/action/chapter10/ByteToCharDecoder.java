package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 字节-字符解码器
 */
public class ByteToCharDecoder extends ByteToMessageDecoder {

    /**
     * 从缓冲中读取数据，并解码为字符
     *
     * @param ctx 管道处理器上下文
     * @param in 缓冲的入站字节数据
     * @param out 解码后的字符消息列表
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 每次从缓冲提取2个字节的Character，添加到字符消息列表
        while (in.readableBytes() >= 2){
            char msg = in.readChar();
            System.out.println("入站数据msg => " + msg);
            out.add(msg);
        }
    }
}
