package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * 将字节转码为整数：继承ReplayingDecoder类
 * 调用readInt等从缓冲中读取具体类型数据的方法时，不需要额外判断可读数据的长度，该类已经默认实现此调用
 * 如果程序读取超出缓冲中可读数据范围，readXxx方法将抛出异常：实际测试没有抛出异常...
 * Void代表不需要状态管理
 */
public class ToIntDecoder extends ReplayingDecoder<Void> {
    /**
     *
     * @param ctx
     * @param in 传入的ByteBuf时ReplayingDecoderByteBuf
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 从入站的ByteBuf(ReplayingDecoderByteBuf)中读取一个int数，并将其添加到解码的消息队列中
        // 如果没有足够的字节可用，将抛出一个错误
        out.add(in.readInt());
    }
}
