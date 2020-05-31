package com.fandou.learning.netty.action.chapter13;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 日志处理器：接收日志内容后，做一些处理，比如保存到数据库或本地磁盘，例子的打印控制台仅作演示
 */
public class LogEventHandler extends SimpleChannelInboundHandler<LogEvent> {

    /**
     * 收到广播的日志事件，在客户端作一些额外的处理：这里仅作演示，打印到控制台
     *
     * @param ctx
     * @param event
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogEvent event) throws Exception {
        // 格式化接收到的日志内容
        StringBuilder builder = new StringBuilder();
        builder.append(event.getReceived());
        builder.append(" [");
        builder.append(event.getSource().toString());
        builder.append("] [");
        builder.append(event.getLogfile());
        builder.append("] : ");
        builder.append(event.getMsg());

        // 打印日志内容到控制台
        System.out.println(builder.toString());
    }

    /**
     * 处理异常
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
