package com.fandou.learning.netty.core.chapter7;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 简单的聊天室服务器端
 */
public class ChatServer {
    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatServer.class);

    /**
     * 聊天室分组
     * 对连接到聊天室服务器的所有客户端channel进行分组，这里只创建一个分组，接下来会调用它的方法来演示广播聊天内容
     */
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    /**
     * 绑定端口，启动聊天室服务端
     *
     * @param port 将被绑定的服务端本地主机端口号
     * @throws Exception
     */
    public void start(int port) throws Exception {
        logger.info("开始启动聊天室服务器...");

        // 声明接收者器事件循环组EventLoopGroup，负责接受客户端连接请求，
        // 构造器参数nThreads表示内部声明了1个只有1个元素的事件执行器EventExecutor数组（事件循环EventLoop数组），
        // EventLoopGroup也是一个线程池。
        // 事件循环EventLoop，顾名思义，负责轮询选择器Selector中的就绪事件，
        // EventLoop继承了EventExecutor，所以，它同时也负责执行轮询过程中其自身提交的对这些事件感兴趣的任务
        EventLoopGroup acceptorGroup = new NioEventLoopGroup(1);

        // 声明客户端事件循环组，负责处理除了连接请求之外的其它客户端请求，
        // 构造器参数nThreads表示内部声明了1个有10个元素的事件执行器数组
        EventLoopGroup clientGroup = new NioEventLoopGroup(10);

        try{
            // 服务器引导，提供链式方法调用设置事件循环组、通道类型、通道处理器、通道选项、通道属性等参数，
            // 让开发人员可以很容易就启动/创建一个服务端套接字通道ServerSocketChannel
            ServerBootstrap server = new ServerBootstrap();

            // 设置接收器事件循环组和客户端事件循环组参数
            server.group(acceptorGroup,clientGroup)
                    // 设置接收器事件循环组中事件循环关联的的套接字通道类型参数，
                    // 对于服务器端，用于处理接收请求的事件循环通道类型为NioServerSocketChannel
                    .channel(NioServerSocketChannel.class)
                    // 设置接收器事件循环关联的通道初始化器/处理器参数
                    //.handler(new ChatServerInitializer(channelGroup))
                    // 设置客户端事件循环关联的通道初始化器/处理器参数
                    .childHandler(new ChatServerInitializer(channelGroup))
                    // 设置服务端绑定的本地主机端口
                    // .localAddress(port)
                    // 设置接收器事件循环关联的通道选项参数
                    .option(ChannelOption.SO_BACKLOG,128)
                    // 设置客户端事件循环关联的通道选项参数：设置为长连接
                    .childOption(ChannelOption.SO_KEEPALIVE,true);

            // 同步绑定本地主机端口，开启一个acceptor线程（关联acceptorGroup中的一个事件执行器）轮询就绪事件和执行处理IO事件的任务队列，准备接收客户端的连接请求
            // 绑定操作的内部执行流程：
            // 1.首先是创建并初始化服务器端套接字通道NioServerSocketChannel
            // 初始化过程中，在为服务器端套接字通道NioServerSocketChannel的通道管道pipeline添加通道处理器时，由于通道还未注册，会创建一个注册后才调用的触发handlerAdded事件的任务。
            // 2.接着，注册服务器端套接字通道NioServerSocketChannel
            // 将服务器端套接字通道NioServerSocketChannel注册到事件循环组（内部的事件循环即事件执行器关联）的选择器Selector上。
            // 首次注册，由于事件循环中的线程和当前线程不在同一个线程，所以会创建一个【注册任务】，开启一个新线程完成【注册任务】。
            // 而开启的这个线程，便是第1个acceptor线程，其被acceptorGroup中的事件执行器所持有。
            // 每次开启一个新的线程，都会启动事件轮询（即死循环），对感兴趣的事件或提交的任务进行处理。对于acceptor线程，其将会监听OP_ACCEPT事件。
            // 通道注册成功后，将会执行步骤1中创建的任务，于是触发handlerAdded事件，最后channelRegistered、channelActive等事件。
            // 3.绑定本地主机端口
            // 将调用NIO原生API绑定为通道绑定本地主机端口。
            // 绑定操作完成，并(自动）注册了感兴趣的事件即OP_READ事件，步骤2的acceptor线程已经启动了事件轮询，到此，服务器端已经可以接受来自客户端的连接请求了，
            // 服务器正式启动，开始提供服务。
            ChannelFuture future = server.bind(port).sync();

            logger.info("聊天室服务器启动成功：{}",port);

            // 监听通道关闭
            future.channel().closeFuture().sync();
        }
        finally{
            // 优雅关闭主线程和工作线程的轮询
            clientGroup.shutdownGracefully();
            acceptorGroup.shutdownGracefully();
            logger.info("聊天室服务器已关闭：{}",port);
        }
    }
}
