package com.fandou.learning.netty.core.chapter15.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.ThreadLocalRandom;

@ChannelHandler.Sharable
public class PerformanceServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public static final ChannelHandler INSTANCE = new PerformanceServerHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ByteBuf data = Unpooled.directBuffer();
        data.writeBytes(msg);

        // 这里将占用线程比较多的时间，优化上可以另外在一个线程池区处理
        Object result = getResult(data);
        ctx.channel().writeAndFlush(result);

    }

    /**
     * 模拟区数据库获取数据
     *
     * @param data
     * @return
     */
    protected Object getResult(ByteBuf data) {

        // 获取一个随机数配合计算每次查询所需要消耗的时间
        int level = ThreadLocalRandom.current().nextInt(1,1000);

        // 每次查询数据库需要的时间，用来作为QPS的参考数据
        int time;

        // 计算出每次响应需要的时间
        if(level <= 900){
            time = 1; // 1ms
        }
        else if(level <= 950){
            time = 10; // 10ms
        }
        else if(level <= 990){
            time = 100; // 100ms
        }
        else {
            time = 1000; // 1000ms
        }

        // 使用线程休眠时间来模拟查询数据库消耗的时间
        try{
            Thread.sleep(time);
        }
        catch (Exception ex){
            //
        }

        return data;
    }
}
