package com.fandou.learning.netty.core.chapter13.server.handler;

import com.fandou.learning.netty.core.chapter13.processor.IMMessageProcessor;
import com.fandou.learning.netty.core.chapter13.protocol.IMMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 终端聊天室处理器
 */
public class TerminalServerHandler extends SimpleChannelInboundHandler<IMMessage> {

    /**
     * logback日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TerminalServerHandler.class);

    /**
     * 即时消息处理器
     */
    private IMMessageProcessor processor = new IMMessageProcessor();

    /**
     *  收到终端聊天室用户发来的消息
     *
     * @param ctx           通道处理器上下文
     * @param msg           消息
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMMessage msg) throws Exception {
        // 收到终端聊天室用户发来的消息，可能是登录|聊天|登出等消息，控制台终端的用户暂不支持发送鲜花
        // 具体如何响应，统统交给即时消息处理器processor处理
        processor.sendMessage(ctx.channel(),msg);
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
        logger.info(ctx.channel().remoteAddress() + "发生异常 -> ");
        cause.printStackTrace();
        ctx.close();
    }
}
