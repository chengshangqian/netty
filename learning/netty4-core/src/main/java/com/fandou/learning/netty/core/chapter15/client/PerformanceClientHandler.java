package com.fandou.learning.netty.core.chapter15.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class PerformanceClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    // 日志
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PerformanceClientHandler.class);

    // 处理器实例
    public static final ChannelHandler INSTANCE = new PerformanceClientHandler();

    // 开始时间
    private static AtomicLong beginTime = new AtomicLong(0);

    // 总响应时间
    private static AtomicLong totalResponseTime = new AtomicLong(0);

    // 总请求次数
    private static AtomicLong totalRequest = new AtomicLong(0);

    public static final Thread THREAD = new Thread(){

        @Override
        public void run() {
            try {
                for (; ; ) {
                    // 累计时间
                    long duration = System.currentTimeMillis() - beginTime.get();

                    if (duration != 0) {
                        // 平均每秒响应请求次数
                        long qps = 1000 * totalRequest.get() / duration;
                        // 平均每次请求的响应时间
                        float responseTime = ((float) totalResponseTime.get()) / totalRequest.get();
                        logger.info("QPS:{},平均响应时间：{}ms...", qps, responseTime);

                        // 每隔2秒打印一次
                        Thread.sleep(2000L);
                    }
                }
            }catch(Exception exception){
                //
            }
        }
    };

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.executor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                ByteBuf byteBuf = ctx.alloc().ioBuffer();
                // 通道激活，发送当前系统时间
                byteBuf.writeLong(System.currentTimeMillis());
                ctx.channel().writeAndFlush(byteBuf);
            }
        },0,1, TimeUnit.SECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // 获取计算本次请求的响应时间
        totalResponseTime.addAndGet(System.currentTimeMillis() - msg.readLong());
        // 请求次数自增1
        totalRequest.incrementAndGet();

        // 首次开始启动线程监控
        if(beginTime.compareAndSet(0,System.currentTimeMillis())){
            THREAD.start();
        }
    }
}
