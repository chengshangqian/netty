package com.fandou.learning.netty.core.chapter13.server.handler;

import com.fandou.learning.netty.core.chapter13.protocol.IMDecoder;
import com.fandou.learning.netty.core.chapter13.protocol.IMEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 聊天室服务端初始化器
 */
public class ChatServerInitializer extends ChannelInitializer<SocketChannel> {

    // websocket的uri
    private String imUri = WebSocketServerHandler.DEFAULT_IM_URI;

    /**
     * 初始化通道
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 获取pipeline
        ChannelPipeline pipeline = ch.pipeline();

        // 添加自定义编码器
        pipeline.addLast(new IMEncoder());
        pipeline.addLast(new IMDecoder());

        // 处理终端客户端请求
        pipeline.addLast(new TerminalServerHandler());

        // 处理普通http请求
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpServerHandler(imUri));

        // 处理websocket客户端请求
        // checkStartsWith设置是否使用开头匹配的方式校验ws的uri,默认false，将用全等检查。
        // 如果需要ws的uri可以带参数，则设置为true。注意，如果处理http升级为websocket的处理器，要校验，应该保持一致
        pipeline.addLast(new WebSocketServerProtocolHandler(imUri,false));
        pipeline.addLast(new WebSocketServerHandler());
    }

    public ChatServerInitializer imUri(String imUri){
        this.imUri = imUri;
        return this;
    }

    public String imUri(){
        return imUri;
    }
}
