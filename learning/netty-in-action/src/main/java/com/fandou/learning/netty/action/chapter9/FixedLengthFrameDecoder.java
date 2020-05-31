package com.fandou.learning.netty.action.chapter9;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 定长帧解码器
 * 用于演示如何使用EmbeddedChannel对自定义的ChannelHandler进行测试
 */
public class FixedLengthFrameDecoder extends ByteToMessageDecoder {

    /**
     * 定义帧的长度
     */
    private final int frameLength;

    /**
     * 构造器，初始化帧长度
     * @param frameLength
     */
    public FixedLengthFrameDecoder(int frameLength) {
        this.frameLength = frameLength;
    }

    /**
     * 解码
     * @param ctx 管道处理器
     * @param in 管道中入站数据
     * @param out 保存解码后的消息的消息列表
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够字节可以被读取
        // 一旦缓冲区可读数据大于约定的帧长度，则进行按照约定的帧长度进行读取出来，添加到已被解码的消息列表out中
        while( in.readableBytes() >= frameLength ){
            ByteBuf frame = in.readBytes(frameLength);
            out.add(frame);
        }
    }
}
