package com.fandou.learning.netty.action.chapter9;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;

/**
 * 最大帧限制解码器
 * 如果入站的帧超出解析所能允许的每帧大小，将抛出异常
 */
public class FrameChunkDecoder extends ByteToMessageDecoder {

    // 定义支持解析的帧的大小：即每个帧的大小，单位字节
    private final int maxFrameSize;

    /**
     * 初始化帧的最大长度
     * @param maxFrameSize
     */
    public FrameChunkDecoder( int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 当前帧可读的字节数
        int readableBytes = in.readableBytes();

        // 如果大于解码器约定的每帧容许的最大字节数，将抛出异常
        if( readableBytes > maxFrameSize){
            // 清空缓冲
            in.clear();
            throw new TooLongFrameException("解码失败，发送的帧允许大小[" + maxFrameSize + "]，实际大小[" + readableBytes + "].");
        }

        // 读取出来，然后将数据添加到解码后的消息列表中(这里仅演示，不做任何改变)
        ByteBuf buf = in.readBytes(readableBytes);
        out.add(buf);
    }
}
