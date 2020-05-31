package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;

import java.util.List;

public class SafeByteToMessageDecoder extends ByteToMessageDecoder {

    /**
     * 每帧的最大字节数
     */
    private static final int MAX_FRAME_SIZE = 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 缓冲区中可读字节数
        int readable = in.readableBytes();

        // 检查是否超出定义的最大字节数
        if(readable > MAX_FRAME_SIZE){
            // 超出将跳过所有的可读字节，然后抛出异常通知ChannelHandler
            in.skipBytes(readable);
            throw new TooLongFrameException("帧的长度[" + readable + "]超出系统约定的最大长度[" + MAX_FRAME_SIZE+ "]...");
        }

        /*** 解码处理,下面仅作演示：解析为UTF8编码格式的字符串 ***/

        // 从缓冲中读取可读的字节数
        byte[] dst = new byte[readable];
        in.readBytes(dst);

        // 将字节转换为UTF-8编码格式的字符串
        out.add(new String(dst, CharsetUtil.UTF_8));
    }
}
