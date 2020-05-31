package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ChatClientInitializer extends ChannelInitializer<SocketChannel> {
    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatClient.class);

    /**
     * 初始化处理channel：将处理器添加到ChannelPipeline中，以对入站和出站消息帧即聊天内容进行处理
     * @param ch            the {@link Channel} which was registered. 注册绑定到ChannelPipeline的Channel
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        logger.debug("================> 调用initChannel");
        /**
         * 获取channel中的pipeline
         */
        ChannelPipeline pipeline = ch.pipeline();

        logger.debug("================> pipeline.addLast：StringEncoder");
        /**
         * 添加字符串编码器：将客户端发送的字符串聊天内容编码为ByteBuf（字节码缓冲）发送
         */
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

        logger.debug("================> pipeline.addLast：StringDecoder");
        /**
         * 添加字符串解码器：将入站的ByteBuf聊天内容解码为字符串
         */
        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

        logger.debug("================> pipeline.addLast：ChatClientHandler");
        /**
         * 添加聊天业务的真正处理器，此处为仅作为演示
         * 1.连接聊天服务器成功时，向聊天室的所有用户发送问候语
         * 2.收到来自聊天室其它用户的消息（从聊天室服务器发送过来），打印到控制台
         */
        pipeline.addLast(new ChatClientHandler("张三"));
    }
}
