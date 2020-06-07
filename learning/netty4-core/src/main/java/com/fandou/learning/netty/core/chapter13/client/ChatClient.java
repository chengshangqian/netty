package com.fandou.learning.netty.core.chapter13.client;

import com.fandou.learning.netty.core.chapter13.client.handler.ChatClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 控制台聊天室客户端
 */
public class ChatClient {

    public static final String TERMINAL = Terminal.CONSOLE.getName();

    /**
     * 日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatClient.class);

    /**
     * 控制台聊天室初始化器
     */
    private ChatClientInitializer initializer ;

    /**
     * 指定一个用户昵称创建一个控制台聊天室客户端
     *
     * @param nickname 用户昵称
     */
    public ChatClient(String nickname){
        initializer = new ChatClientInitializer(nickname);
    }

    /**
     * 连接远程主机地址
     *
     * @param host 远程主机地址
     * @param port 远程主机端口
     */
    public void connect(String host,int port){
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap client = new Bootstrap();
            client.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(host,port)
                    .option(ChannelOption.SO_KEEPALIVE,true)
                    .handler(initializer);
            ChannelFuture future = client.connect().sync();
            logger.info("客户端已经启动:{}:{}...",host,port);
            future.channel().closeFuture().sync();
        } catch (Exception ex){
            logger.info("发生异常: ->");
            ex.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * 启动控制台聊天室客户端
     *
     * @param args
     */
    public static void main(String[] args) {
        new ChatClient("Ahin").connect("127.0.0.1",8080);
    }
}
