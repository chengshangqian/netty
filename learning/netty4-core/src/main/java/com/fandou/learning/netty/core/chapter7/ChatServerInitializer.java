package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 聊天室服务器客户端通道初始化器
 * 当有新的客户端连接过来时，将为客户端连接对应的通道进行初始化，添加处理器以处理消息
 */
public class ChatServerInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * 客户端通道分组
     */
    private final ChannelGroup channelGroup;

    /**
     * 指定通道分组
     *
     * @param channelGroup 通道分组
     */
    public ChatServerInitializer(final ChannelGroup channelGroup){
        this.channelGroup = channelGroup;
    }

    /**
     * 初始化客户端连接通道
     *
     * @param ch  被注册的通道，即客户端连接通道
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 获取通道管道pipeline
        ChannelPipeline pipeline = ch.pipeline();

        // 添加字符串编解码器
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

        // 添加聊天室处理器，以处理消息：主要是广播消息
        pipeline.addLast(new ChatServerHandler(channelGroup));
    }
}
