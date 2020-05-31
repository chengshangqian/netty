package com.fandou.learning.netty.core.chapter7;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;

public class ChatServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 聊天组
     */
    private final ChannelGroup channelGroup;

    public ChatServerHandler(ChannelGroup channelGroup){
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String msg = "新用户加入[" + ctx.channel().id().asShortText() + "]加入了聊天室.";
        System.out.println(msg);

        // 通知客户端组中其它的客户端，有新的客户端上线/加入聊天室
        channelGroup.writeAndFlush(msg);

        // 将新的客户端加入到客户端组中，后续其将可以接收到其新的消息
        channelGroup.add(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 可以自定义欢迎帧的编解码器，检测是否时欢迎帧 TODO
        // ctx.writeAndFlush("欢迎" + msg + "加入!");

        // 普通聊天内容
        String message = "用户[" + ctx.channel().id().asShortText() + "]说：" + msg;
        System.out.println(message);

        // 群发
        channelGroup.writeAndFlush(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
