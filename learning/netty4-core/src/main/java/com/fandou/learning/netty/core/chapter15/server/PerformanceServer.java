package com.fandou.learning.netty.core.chapter15.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 应用性能调优示例服务器
 */
public class PerformanceServer {
    // 端口号
    private static final int port = 8000;

    // 日志
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PerformanceServer.class);

    // 启动服务器
    public static void main(String[] args) {
        logger.info("正在启动应用性能调优示例服务器...");
        EventLoopGroup acceptorGroup = new NioEventLoopGroup();
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        EventLoopGroup businessGroup = new NioEventLoopGroup(1000);
        ServerBootstrap server = new ServerBootstrap();
        server.group(acceptorGroup,clientGroup);
        server.channel(NioServerSocketChannel.class);
        server.childOption(ChannelOption.SO_REUSEADDR,true);
        server.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                // 发送一个Long型的时间戳，使用定长解码器解码，然后交给PerformanceServerHandler最后响应
                pipeline.addLast(new FixedLengthFrameDecoder(Long.BYTES));
                // 另外一个线程池？
                pipeline.addLast(businessGroup,PerformanceServerThreadPoolHandler.INSTANCE);
            }
        });
        server.bind(port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("成功绑定端口:{}" + port);
            }
        });
        logger.info("应用性能调优示例服务器启动成功...");
    }
}
