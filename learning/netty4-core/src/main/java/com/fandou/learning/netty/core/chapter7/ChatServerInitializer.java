package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

public class ChatServerInitializer extends ChannelInitializer<SocketChannel> {

    private final ChannelGroup channelGroup;

    public ChatServerInitializer(final ChannelGroup channelGroup){
        this.channelGroup = channelGroup;
    }
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(new ChatServerHandler(channelGroup));
    }
}
