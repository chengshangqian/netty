package com.fandou.learning.netty.kaikeba.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.CharsetUtil;

/**
 * 自定义解码器
 * 将ByteBuf解码为FandouBean对象
 */
public class FandouDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * 头部总长度（headers)：包括了2头部header（每个header占1字节）和1个长度域（保存表示内容长度的4字节宽的整数），共6个字节
     */
    private static final int HEADER_SIZE = 6;

    /**
     * 初始化解码器
     * @param maxFrameLength
     * @param lengthFieldOffset
     * @param lengthFieldLength
     * @param lengthAdjustment
     * @param initialBytesToStrip
     * @param failFast
     */
    public FandouDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    /**
     * 实现解码规则
     * @param ctx
     * @param in
     * @return
     * @throws Exception
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 调用父类先解码一次：返回一个数据包（帧），也有可能是null
        in = (ByteBuf)super.decode(ctx,in);

        if(null == in){
            return null;
        }

        // 检查返回的数据包（帧），如果小于头部内容，被认为是不符合的数据包，抛出异常（允许service为null)
        if(in.readableBytes() < HEADER_SIZE){
            throw new Exception("缺少头部或头部字节数长度不足.");
        }

        // 读取第1个头部字节内容
        byte application = in.readByte();

        // 读取第2个头部字节内容
        byte category = in.readByte();

        // 读取长度域（4个字节）内容
        int length = in.readInt();

        // 检查内容长度与长度域中的值是否一致，如果不一致，抛出异常
        int serviceLength = in.readableBytes();
        // 需另外考虑service为null或length=0的情况
        if(serviceLength != length){
            // 约定标记长度域实际长度需要一致
            throw new Exception("标记的长度不符合实际长度...");
        }

        // 读取内容即service地址
        byte[] service = new byte[serviceLength];
        in.readBytes(service);

        // 最后使用解码后的内容创建一个FandouBean对象返回
        return new FandouBean(application,category,length,new String(service, CharsetUtil.UTF_8));
    }

}
