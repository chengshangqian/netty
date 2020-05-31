package com.fandou.learning.netty.core.chapter7;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;

public class ChatServer {
    /**
     * 客户端组：保存连接到聊天室服务器的客户端channel
     */
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    public void start(int port) throws Exception {

        System.out.println("应用启动线程名称 => " + Thread.currentThread().getName());

        // 创建事件循环组：主线程，接收客户端连接请求
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 创建事件循环组：工作线程，负责处理和客户端的具体交互
        EventLoopGroup workerGroup = new NioEventLoopGroup(10);

        try{
            /**
             * 创建服务器引导
             */
            ServerBootstrap server = new ServerBootstrap();

            // 配置主线程和工作线程参数
            server.group(bossGroup,workerGroup)
                    // 设置主线程的套接字通道类型参数
                    .channel(NioServerSocketChannel.class)
                    // 设置主线程初始化器/处理器
                    //.handler(new ChannelInitializer(channelGroup))
                    // 设置工作线程初始化器参数
                    .childHandler(new ChatServerInitializer(channelGroup))
                    // 设置主线程通道选项参数
                    .option(ChannelOption.SO_BACKLOG,128)
                    // 设置工作线程通道参数
                    .childOption(ChannelOption.SO_KEEPALIVE,true);

            /**
             * 同步绑定本地通讯端口，启动服务器
             */
            ChannelFuture future = server.bind(port).sync();
            System.out.println("服务器启动...");

            future.channel().closeFuture().sync();
        }
        finally{
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("服务已经关闭.");
        }
    }
}
