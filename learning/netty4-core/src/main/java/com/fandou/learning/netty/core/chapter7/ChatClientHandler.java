package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 聊天客户端处理器
 */
public class ChatClientHandler extends ChannelInboundHandlerAdapter {

    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatClientHandler.class);

    /**
     * 用户昵称
     */
    private final String nickname;

    /**
     * 指定用户昵称，实例化处理器
     *
     * @param nickname
     */
    public ChatClientHandler(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 监听handlerAdded事件，当前处理器的被添加到pipeline中时，触发此方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("收到handlerAdded事件，继续传递...");
        super.handlerAdded(ctx);
    }

    /**
     * 监听channelActive事件，当通道激活即通道打开并连接成功时，触发此方法
     *
     * 发送当前用户昵称和问候语
     *
     * @param ctx Channel和ChannelPipeline上下文
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("收到channelActive事件，发送客户端用户昵称...");
        // 发送连接客户端的用户昵称到服务端，由服务端广播给其它客户端，通知新用户加入
        // 可以自定义专门的问候/欢迎帧编解码器
        ctx.writeAndFlush(nickname);
    }

    /**
     * 监听channelRegistered事件，当通道注册成功时，触发此方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("收到channelRegistered事件，继续传递...");
        super.channelRegistered(ctx);
    }

    /**
     *
     * 监听可读事件channelRead，当接收到来自服务器端的消息其可以进行读取时，触发此方法
     *
     * 在控制台打印接收到服务器转发过来的其它用户发来的聊天内容
     *
     * @param ctx Channel和ChannelPipeline上下文
     * @param msg 服务端发送过来的消息内容：已经过StringDecoder编码为字符串
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("{}", msg.toString());
    }

    /**
     * 监听异常事件，当发生异常时，触发此方法
     *
     * @param ctx Channel和ChannelPipeline上下文
     * @param cause 异常信息
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
