package com.fandou.learning.netty.core.chapter15.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ChannelHandler.Sharable
public class PerformanceServerThreadPoolHandler extends PerformanceServerHandler {
    public static final ChannelHandler INSTANCE = new PerformanceServerThreadPoolHandler();

    private static ExecutorService threadPool = Executors.newFixedThreadPool(1000);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ByteBuf data = Unpooled.directBuffer();
        data.writeBytes(msg);

        // 从当前workGroup/businessGroup中的线程中，另外开启一个线程池区执行任务
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                Object result = getResult(data);
                ctx.channel().writeAndFlush(result);
            }
        });
    }
}
