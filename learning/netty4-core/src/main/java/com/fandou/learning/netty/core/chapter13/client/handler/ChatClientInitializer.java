package com.fandou.learning.netty.core.chapter13.client.handler;

import com.fandou.learning.netty.core.chapter13.protocol.IMDecoder;
import com.fandou.learning.netty.core.chapter13.protocol.IMEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * 控制台终端聊天室初始化器
 */
public class ChatClientInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 指定昵称实例化初始化器
     *
     * @param nickname 用户昵称
     */
    public ChatClientInitializer(String nickname){
        this.nickname = nickname;
    }

    /**
     * 初始化管道
     * @param ch 被注册的通道
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 获取pipeline
        ChannelPipeline pipeline = ch.pipeline();

        // 添加自定义编码器
        pipeline.addLast(new IMEncoder());
        pipeline.addLast(new IMDecoder());
        pipeline.addLast(new ChatClientHandler(nickname));
    }
}
