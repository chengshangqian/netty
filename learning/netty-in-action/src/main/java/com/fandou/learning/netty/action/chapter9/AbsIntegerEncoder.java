package com.fandou.learning.netty.action.chapter9;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * 绝对值编码器
 * 将负数编码为绝对值
 */
public class AbsIntegerEncoder  extends MessageToMessageEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否与足够的字节数用来编码：负数使用4个字节保存
        while ( in.readableBytes() >= 4 ) {
            // 从输入的ByteBuf中读取一个整数，调用Math.abs方法转换为绝对值
            int value = Math.abs(in.readInt());

            // 将绝对值整数写入到编码消息输出列表中
            out.add(value);
        }
    }
}
