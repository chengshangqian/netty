package com.fandou.learning.netty.core.chapter13.server;

import com.fandou.learning.netty.core.chapter13.server.handler.ChatServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 聊天室服务端
 */
public class ChatServer {

    /**
     * logback日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatServer.class);

    public void start(int port){
        // 创建acceptor和工作线程EventLoopGroup
        EventLoopGroup acceptorGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            // 服务端引导
            ServerBootstrap server = new ServerBootstrap();
            server.group(acceptorGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .localAddress(port)
                    .childHandler(new ChatServerInitializer());
            ChannelFuture future = server.bind().sync();
            logger.info("服务器已经启动:{}...",port);
            future.channel().closeFuture().sync();
        } catch (Exception ex){
            logger.info("发生异常: ->");
            ex.printStackTrace();
        }
        finally {
            workerGroup.shutdownGracefully();
            acceptorGroup.shutdownGracefully();
        }
    }
}
