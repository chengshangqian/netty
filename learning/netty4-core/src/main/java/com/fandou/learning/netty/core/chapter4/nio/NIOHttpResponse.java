package com.fandou.learning.netty.core.chapter4.nio;

import com.fandou.learning.netty.core.chapter4.server.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * HTTP响应
 */
public class NIOHttpResponse implements Response {
    /**
     * Netty封装的客户端channel的请求上下文
     */
    private final ChannelHandlerContext ctx;

    /**
     * Netty封装的客户端的请求
     */
    private final HttpRequest request;

    /**
     * 初始化HTTP响应
     *
     * @param ctx Netty封装的客户端channel的请求上下文
     * @param request Netty封装的客户端的请求
     */
    public NIOHttpResponse(ChannelHandlerContext ctx, HttpRequest request){
        this.ctx = ctx;
        this.request = request;
    }

    /**
     * 发送响应内容即请求的处理结果给客户端
     *
     * @param message 发送给客户端的消息内容
     * @throws Exception
     */
    @Override
    public void write(String message) throws Exception{

        // 不允许发送空内容
        if(null == message || message.isEmpty()){
            return;
        }

        try {
            // 将发送的消息放入到缓冲区中
            ByteBuf messageByteBuf = Unpooled.wrappedBuffer(message.getBytes(CharsetUtil.UTF_8));

            // HTTP协议版本
            HttpVersion version = HttpVersion.HTTP_1_1;

            // 状态
            HttpResponseStatus status = HttpResponseStatus.OK;

            // 创建HTTP响应
            FullHttpResponse response = new DefaultFullHttpResponse(version, status, messageByteBuf);

            // 设置HTTP响应头：内容类型CONTENT_TYPE及其编码
            response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html;charset=" + CharsetUtil.UTF_8);

            // 写入管道channel，等待发送
            ctx.write(response);
        }
        finally {
            // 将缓冲区消息刷新发送给客户端
            ctx.flush();
            ctx.close();
        }
    }
}
