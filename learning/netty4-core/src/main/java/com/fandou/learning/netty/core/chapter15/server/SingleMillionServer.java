package com.fandou.learning.netty.core.chapter15.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public final class SingleMillionServer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SingleMillionServer.class);

    public static final int BEGIN_PORT = 8000;
    public static final int END_PORT = 8100;

    public static void main(String[] args) {
        new SingleMillionServer().start(BEGIN_PORT,END_PORT);
    }

    private void start(int beginPort, int endPort) {
        logger.info("正在启动单机百万连接服务器...");
        EventLoopGroup acceptorGroup = new NioEventLoopGroup();
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(acceptorGroup,clientGroup);
        server.channel(NioServerSocketChannel.class);
        server.childOption(ChannelOption.SO_REUSEADDR,true);
        server.childHandler(new SingleMillionConnectionCountHandler());
        for (int i = 0; i <= (endPort - beginPort) ; i++) {
            final int port = beginPort + i;
            server.bind(port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.info("成功绑定端口:{}" + port);
                }
            });
        }
        logger.info("单机百万连接服务器启动成功...");
    }
}
