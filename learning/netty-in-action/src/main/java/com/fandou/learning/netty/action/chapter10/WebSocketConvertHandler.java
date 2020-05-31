package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.*;

import java.util.List;

/**
 * WebSocket双向通信编解码处理器
 */
public class WebSocketConvertHandler extends MessageToMessageCodec<WebSocketFrame,WebSocketConvertHandler.MyWebSocketFrame> {

    /**
     * 将自定义的WebSocket帧编码为Netty实现的WebSocket帧即规范的WebSocket帧
     *
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, MyWebSocketFrame msg, List<Object> out) throws Exception {
        ByteBuf payload = msg.getData().duplicate().retain();
        switch (msg.getType()){
            case BINARY:
                out.add(new BinaryWebSocketFrame(payload));
                break;
            case TEXT:
                out.add(new TextWebSocketFrame(payload));
                break;
            case PING:
                out.add(new PingWebSocketFrame(payload));
                break;
            case PONG:
                out.add(new PongWebSocketFrame(payload));
                break;
            case CLOSE:
                out.add(new CloseWebSocketFrame(true,0,payload));
                break;
            case CONTINUATION:
                out.add(new ContinuationWebSocketFrame(payload));
                break;
            default:
                throw new IllegalStateException("不支持的WebSocket消息 => " + msg);
        }
    }

    /**
     * 将规范的WebSocket帧解码为自定义的WebSocket帧
     *
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
        ByteBuf payload = msg.content().duplicate().retain();
        if(msg instanceof BinaryWebSocketFrame){
            out.add(new MyWebSocketFrame(MyWebSocketFrame.FrameType.BINARY,payload));
        }
        else if(msg instanceof TextWebSocketFrame){
            out.add(new MyWebSocketFrame(MyWebSocketFrame.FrameType.TEXT,payload));
        }
        else if(msg instanceof PingWebSocketFrame){
            out.add(new MyWebSocketFrame(MyWebSocketFrame.FrameType.PING,payload));
        }
        else if(msg instanceof PongWebSocketFrame){
            out.add(new MyWebSocketFrame(MyWebSocketFrame.FrameType.PONG,payload));
        }
        else if(msg instanceof CloseWebSocketFrame){
            out.add(new MyWebSocketFrame(MyWebSocketFrame.FrameType.CLOSE,payload));
        }
        else if(msg instanceof ContinuationWebSocketFrame){
            out.add(new MyWebSocketFrame(MyWebSocketFrame.FrameType.CONTINUATION,payload));
        }
        else {
            throw new IllegalStateException("不支持的WebSocket消息 => " + msg);
        }
    }

    /**
     * 自定义WebSocket帧
     */
    public static final class MyWebSocketFrame {

        public enum FrameType {
            BINARY,CLOSE,PING,PONG,TEXT,CONTINUATION
        }

        private final FrameType type;
        private final ByteBuf data;

        public MyWebSocketFrame(FrameType type,ByteBuf data){
            this.type = type;
            this.data = data;
        }

        public FrameType getType() {
            return type;
        }

        public ByteBuf getData() {
            return data;
        }
    }
}
