package com.fandou.learning.netty.core.chapter15.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 应用性能调优示例客户端
 *
 */
public class PerformanceClient {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PerformanceClient.class);

    private static String host = "192.168.8.114";

    public static void main(String[] args) throws Exception{
        if(null != args && args.length > 0){
            host = args[0].trim();
        }

        new PerformanceClient().start(8000);
    }

    private void start(int port) throws Exception {
        logger.info("启动应用性能调优示例客户端...");
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap client = new Bootstrap();
        client.group(group);
        client.channel(NioSocketChannel.class);
        client.option(ChannelOption.SO_REUSEADDR, true);
        client.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new FixedLengthFrameDecoder(Long.BYTES));
                pipeline.addLast(PerformanceClientHandler.INSTANCE);
            }
        });

        // 每秒向客户端发起1000次连接请求
        for (int i = 0; i < 1000; i++) {
            client.connect(host, port).get();
        }
    }
}
