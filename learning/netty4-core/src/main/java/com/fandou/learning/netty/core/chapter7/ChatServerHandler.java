package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 聊天室服务处理器
 */
public class ChatServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatServerHandler.class);


    /**
     * 聊天分组（客户端通道分组）
     */
    private final ChannelGroup channelGroup;

    /**
     * 指定聊天分组，实例化处理器
     *
     * @param channelGroup 聊天分组
     */
    public ChatServerHandler(ChannelGroup channelGroup){
        this.channelGroup = channelGroup;
    }

    /**
     * 监听channelActive事件，当通道被激活即有新的客户端连接时被触发此方法
     *
     * 在控制台打印新用户信息，然后通知聊天室其它人员，有新用户上线。最后将新用户加入聊天室分组
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 新用户加入的消息
        String msg = "新用户加入[" + ctx.channel().id().asShortText() + "]加入了聊天室.";

        // 在控制台打印新用户信息，仅作演示
        logger.info(msg);

        // 向聊天室分组广播新用户加入聊天室的消息
        channelGroup.writeAndFlush(msg);

        // 将新用户加入到聊天室分组中，后续其将可以接收到其新的消息
        channelGroup.add(ctx.channel());
    }

    /***
     * 监听channelRead事件，当有新的消息可读取时，触发此方法
     *
     * 在控制台打印新用户信息，然后将消息广播给聊天室分组中的所有用户
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 可以自定义欢迎帧的编解码器，检测是否时欢迎帧

        // 普通聊天内容
        String message = "用户[" + ctx.channel().id().asShortText() + "]说：" + msg;

        // 在控制台打印消息，仅作演示
        logger.info(message);

        // 广播聊天内容
        channelGroup.writeAndFlush(message);
    }

    /**
     * 监控异常事件
     *
     * 如果发生异常，关闭此当前通道
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
