package com.fandou.learning.netty.core.chapter7;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 聊天室客户端
 */
public class ChatClient {

    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatClient.class);

    /**
     * 连接聊天服务器，加入聊天
     *
     * @param host 聊天服务器主机地址
     * @param port 聊天服务器主机端口
     * @param nickname 客户端用户昵称
     * @return
     */
    public ChatClient connect(String host,int port,String nickname) throws Exception{

        logger.info("开始连接聊天室服务器...");

        // 事件循环组，负责轮询感兴趣的I/O事件和执行相关的任务，
        // 内部持有一个事件执行器数组/事件循环数据区完成所有的事情
        EventLoopGroup group = new NioEventLoopGroup();

        try{
            // 聊天客户端引导，配置连接到服务器的通道参数，然后连接服务器
            Bootstrap client = new Bootstrap();

            // 设置事件循环组group参数
            client.group(group)
                    // 设置与远程主机连接时的通道类型channel参数，
                    // 客户端的话，通道类型使用非阻塞IO通道NioSocketChannel
                    .channel(NioSocketChannel.class)
                    // 设置处理器handler参数，用于处理于服务端的I/O交互，即如何收发聊天内容
                    .handler(new ChatClientInitializer(nickname))
                    // 设置通道的选项：SO_KEEPALIVE表示保持长连接
                    .option(ChannelOption.SO_KEEPALIVE,true);

            // 指定远程主机地址和端口，然后连接聊天服务器，启动客户端
            ChannelFuture future = client.connect(host, port).sync();

            logger.info("连接聊天室服务器成功，聊天室客户端启动...");

            // 监听连接channel关闭
            future.channel().closeFuture().sync();
        }
        finally {
            // 优雅关闭事件轮询
            group.shutdownGracefully();
        }

        return this;
    }
}
