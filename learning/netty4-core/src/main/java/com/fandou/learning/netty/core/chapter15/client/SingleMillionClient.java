package com.fandou.learning.netty.core.chapter15.client;

import com.fandou.learning.netty.core.chapter15.server.SingleMillionServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 单机百万连接客户端
 */
public class SingleMillionClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SingleMillionClient.class);

    private static String host = "127.0.0.1";

    public static void main(String[] args) {
        if(null != args && args.length > 0){
            host = args[0].trim();
        }
        new SingleMillionClient().start(SingleMillionServer.BEGIN_PORT,SingleMillionServer.END_PORT);
    }

    private void start(int beginPort, int endPort) {
        logger.info("启动单机百万连接客户端...");
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap client = new Bootstrap();
        client.group(group);
        client.channel(NioSocketChannel.class);
        client.option(ChannelOption.SO_REUSEADDR,true);
        client.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // 不需要
            }
        });

        int index = 0;
        int port;

        while (!Thread.interrupted()){
            port = beginPort + index++;
            try {
                ChannelFuture channelFuture = client.connect(host, port);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if(!future.isSuccess()){
                            logger.info("连接失败,程序关闭:");
                            logger.info(future.cause());
                            System.exit(0);
                        }
                    }
                });
                channelFuture.get();
            } catch (Exception ex){
                // 不打印
            }

            // 循环连接
            if(port == endPort){
                index = 0;
            }
        }
    }
}
