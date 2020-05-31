package com.fandou.learning.netty.action.chapter12;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;

import java.net.InetSocketAddress;

/**
 * 聊天室服务器端
 */
public class ChatServer {
    /**
     * 客户端组：保存连接到聊天室服务器的客户端channel
     */
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    /**
     * 异步事件循环组
     */
    private final EventLoopGroup group = new NioEventLoopGroup();

    /**
     * 服务器端channel
     */
    private Channel channel;
    
    public ChannelFuture start(InetSocketAddress address){
        // 创建Netty服务器引导
        ServerBootstrap bootstrap = new ServerBootstrap();

        // 设置组、管道以及管道初始化处理器
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(createInitializer(channelGroup));

        // 绑定端口启动服务器
        ChannelFuture future = bootstrap.bind(address);
        System.out.println("聊天室服务端启动...");

        future.syncUninterruptibly();
        channel = future.channel();

        return future;
    }

    /**
     * 创建管道初始化器
     *
     * @param channelGroup
     * @return
     */
    private ChannelInitializer<Channel> createInitializer(ChannelGroup channelGroup) {
        // 创建并返回聊天室初始化器
        return new ChatServerInitializer(channelGroup);
    }

    /**
     * 关闭资源
     */
    public void destroy(){
        if(null != channel){
            channel.close();
        }
        channelGroup.close();
        group.shutdownGracefully();
    }
}
