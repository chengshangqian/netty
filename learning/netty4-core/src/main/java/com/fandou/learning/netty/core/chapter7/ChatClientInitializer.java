package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 聊天室客户端通道初始化器
 */
public class ChatClientInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatClientInitializer.class);

    /**
     * 用户昵称
     */
    private final String nickname;

    /**
     * 指定用户昵称，实例化初始化器
     *
     * @param nickname
     */
    public ChatClientInitializer(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 初始化通道channel
     * 将处理器添加到pipeline中，以对入站和出站消息帧即聊天内容进行处理
     *
     * @param ch 被注册的通道
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 获取通道管道pipeline
        ChannelPipeline pipeline = ch.pipeline();

        // 添加字符串编码器，将客户端发送的字符串聊天内容编码为ByteBuf（字节码缓冲）
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

        // 添加字符串解码器，将入站的ByteBuf聊天内容解码为字符串
        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

        // 添加聊天客户端的业务处理器
        // 连接聊天服务器成功时，向聊天室的所有用户发送问候语，
        // 收到来自聊天室其它用户的消息（从聊天室服务器发送过来），打印到控制台
        logger.info("开始为客户端通道添加处理器handler...");
        pipeline.addLast(new ChatClientHandler(nickname));
        logger.info("结束为客户端通道添加处理器handler...");
    }
}
