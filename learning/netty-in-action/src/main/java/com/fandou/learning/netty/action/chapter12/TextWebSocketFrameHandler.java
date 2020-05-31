package com.fandou.learning.netty.action.chapter12;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * WebSocket文本帧处理器，处理聊天室内容的收和发（广播）
 */
public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    // 连接到聊天室服务器的客户端组
    private final ChannelGroup group;

    // 初始化客户端组
    public TextWebSocketFrameHandler(ChannelGroup group){
        this.group = group;
    }

    /**
     * 重新userEventTriggered以处理自定义事件
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 如果是WebSocket的握手成功协议
        if(evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE){
            // WebSocket双向通信建立成功，则从pipeline中移除HttpRequestHandler
            // 因为后续通信将使用WebSocket协议，不会再使用HTTP请求即不会收到HTTP的消息
            ctx.pipeline().remove(HttpRequestHandler.class);

            // 通知客户端组中其它的客户端，有新的客户端上线/加入聊天室
            group.writeAndFlush(new TextWebSocketFrame("客户端[" + ctx.channel().id().asShortText() + "]加入了聊天室."));

            // 将新的客户端加入到客户端组中，后续其将可以接收到其新的消息
            group.add(ctx.channel());
        }
        else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 读取客户端发送的聊天消息，并广播给其它客户端
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 收到客户端发来的消息，增加msg消息引用计数，然后原样转发/广播给其它所有的客户端
        // group.writeAndFlush(msg.retain());

        // 格式化文本内容再广播给其它客户端
        String client = ctx.channel().id().asShortText();
        String textMsg = msg.text();

        // 有时会接收到很多空消息，待了解原因，先把空消息屏蔽
        if(null != textMsg && !"".equals(textMsg.trim())){
            String formatTextMsg = "客户端[" + client + "]说: " + textMsg;
            System.out.println(formatTextMsg);
            group.writeAndFlush(new TextWebSocketFrame(formatTextMsg));
        }
    }
}
