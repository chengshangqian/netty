package com.fandou.learning.netty.kaikeba.client;

import com.fandou.learning.netty.kaikeba.server.FandouBean;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

/**
 * 自定义编码器
 * 将FandouBean对象编码为ByteBuf
 */
public class FandouEncoder extends MessageToByteEncoder<FandouBean> {

    /**
     * 将FandouBean对象编码为ByteBuf
     * @param ctx
     * @param bean
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, FandouBean bean, ByteBuf out) throws Exception {
        if( null == bean){
            throw new Exception("bean不能为null.");
        }

        // 将FandouBean对象编码为ByteBuf
        out.writeByte(bean.getApplication());
        out.writeByte(bean.getCategory());
        out.writeInt(bean.getLength());
        out.writeBytes(bean.getService().getBytes(CharsetUtil.UTF_8));
    }
}
