package com.fandou.learning.netty.action.chapter13;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;

/**
 * 日志监控器：监控是否有日志发送过来或有日志可以接收
 */
public class LogEventMonitor {
    /**
     * 事件循环组
     */
    private final EventLoopGroup group;

    /**
     * 日志监控器引导
     */
    private final Bootstrap bootstrap;

    /**
     * 监控本地端口
     *
     * @param address 监控本地主机端口
     */
    public LogEventMonitor(InetSocketAddress address) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();

        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST,true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 添加日志解码器
                        pipeline.addLast(new LogEventDecoder());
                        // 添加日志处理器
                        pipeline.addLast(new LogEventHandler());
                    }
                })
                .localAddress(address);
    }

    /**
     * 绑定本地主机端口，启动日志监听器
     *
     * @return
     */
    public Channel bind(){
        return bootstrap.bind().syncUninterruptibly().channel();
    }

    /**
     * 关闭日志监听器资源
     */
    public void stop(){
        group.shutdownGracefully();
    }
}
