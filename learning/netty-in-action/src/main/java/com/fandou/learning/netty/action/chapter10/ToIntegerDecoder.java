package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 将字节转码为整数：继承ByteToMessageDecoder类
 */
public class ToIntegerDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查入站的缓冲数据中是否由4个字节或以上的可读数据
        if(in.readableBytes() >= 4){
            // 从入站的ByteBuf中读取一个int数据，并将其添加到解码的消息List中
            out.add(in.readInt());
        }
    }
}
