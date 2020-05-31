package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ChatClientHandler extends ChannelInboundHandlerAdapter {

    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatClientHandler.class);

    private final String nickname;

    public ChatClientHandler(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.debug("================> 收到handlerAdded事件...");
        super.handlerAdded(ctx);
    }

    /**
     * 连接成功时，立即发送问候语
     * 处理成功连接事件：连接到聊天服务器时，触发此方法
     *
     * @param ctx Channel和ChannelPipeline上下文
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("================> 收到channelActive事件...");
        // 发送连接客户端的用户昵称到服务端，由服务端广播给其它客户端，通知新用户加入
        // 可以自定义专门的问候/欢迎帧编解码器
        ctx.writeAndFlush(nickname);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.debug("================> 收到channelRegistered事件...");
        super.channelRegistered(ctx);
    }

    /**
     * 控制台打印接收到服务器转发过来的其它用户发来的聊天内容
     * 处理消息可读事件：当接收到来自服务器端的消息其可以进行读取时，触发此方法
     *
     * @param ctx Channel和ChannelPipeline上下文
     * @param msg 服务端发送过来的消息内容：已经过StringDecoder编码为字符串
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //System.out.println(msg);
        logger.info(msg.toString());
    }

    /**
     * 处理捕获的异常信息
     * 处理异常事件：当发生异常时，触发此方法
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
