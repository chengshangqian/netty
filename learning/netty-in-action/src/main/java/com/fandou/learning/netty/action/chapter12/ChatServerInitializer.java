package com.fandou.learning.netty.action.chapter12;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 聊天室初始化器
 */
public class ChatServerInitializer extends ChannelInitializer<Channel> {
    /**
     * 连接聊天室的客户端channel组
     */
    private final ChannelGroup group;

    /**
     * 初始化连接聊天室的客户端channel组
     * @param group
     */
    public ChatServerInitializer(ChannelGroup group) {
        this.group = group;
    }

    /**
     * 添加处理器到ChannelPipeline中
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // Http的编解码器
        pipeline.addLast(new HttpServerCodec());

        // 写文件处理器
        pipeline.addLast(new ChunkedWriteHandler());

        // Http聚合器
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));

        // 添加处理Http请求的处理器
        pipeline.addLast(new HttpRequestHandler("/ws"));

        // 添加WebSocket协议处理器
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        // 添加WebSocket协议文本帧的处理器：处理聊天室内容的接收和广播发送
        pipeline.addLast(new TextWebSocketFrameHandler(group));
    }
}
