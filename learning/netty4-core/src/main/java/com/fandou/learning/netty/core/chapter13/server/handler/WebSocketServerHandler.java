package com.fandou.learning.netty.core.chapter13.server.handler;

import com.fandou.learning.netty.core.chapter13.processor.IMMessageProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * WebSocket端聊天室处理器
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 缺省的websocket聊天室uri
     */
    public static final String DEFAULT_IM_URI = "/im";

    /**
     * 日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketServerHandler.class);

    /**
     * 即时消息处理器
     */
    private IMMessageProcessor processor = new IMMessageProcessor();

    /**
     * 收到终端聊天室用户发来的消息
     *
     * @param ctx          通道处理器上下文
     * @param msg          消息
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 收到终端聊天室用户发来的消息，可能是登录|聊天|送花|登出等消息
        // 具体如何响应，统统交给即时消息处理器processor处理
        processor.sendMessage(ctx.channel(),msg.text());
    }

    /**
     * 处理捕获的异常
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info(processor.getAddress(ctx.channel()) + "发生异常 -> ");
        cause.printStackTrace();
        ctx.close();
    }
}
