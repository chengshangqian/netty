package com.fandou.learning.netty.action.chapter12;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 聊天室Http请求处理器
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * 聊天室聊天信息通信URI
     */
    private final String wsUri;

    /**
     * 聊天室首页文件
     */
    private static final File INDEX;

    /**
     * 加载聊天室首页index.html
     * 当有用户从浏览器访问聊天室时，将向用户响应首页内容
     */
    static {
        URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try{
            String path = location.toURI() + "index.html";
            path = !path.contains("file:") ? path : path.substring(5);
            INDEX = new File(path);
        }
        catch (URISyntaxException ex){
            throw new IllegalStateException("无法加载index.html聊天室首页",ex);
        }
    }

    /**
     * 初始化基于WebSocket协议的聊天室的相对访问地址
     * @param wsUri
     */
    public HttpRequestHandler(String wsUri) {
        this.wsUri = wsUri;
    }

    /**
     * 对用户的请求进行响应：如果是聊天室中的请求，将转发请求；如果是访问聊天室首页则响应首页内容
     * @param ctx
     * @param request
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 如果当前请求是WebSocket请求
        if(wsUri.equalsIgnoreCase(request.uri())){
            // 增加引用计数并将请求传递给下一个处理器（WebSocket帧的处理器）进行处理
            ctx.fireChannelRead(request.retain());
        }
        // 请求访问聊天室首页
        else {
            // 如果是100请求：继续
            if(HttpUtil.is100ContinueExpected(request)){
                send100Continue(ctx);
            }

            /*** 发送聊天室首页 ***/
            // 准备读取聊天室首页index.html文件内容
            RandomAccessFile file = new RandomAccessFile(INDEX,"r");

            // 创建响应并设置响应的内容类型头信息
            HttpResponse response = new DefaultHttpResponse(request.protocolVersion(),HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html; charset=UTF-8");

            // 检查是否请求了keep-alive
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if(keepAlive){
                // 如果是keep-alive请求，设置对应的响应头信息
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH,file.length());
                response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
            }

            // 将响应写入给客户端
            ctx.write(response);

            // 将首页内容写到客户端
            if(ctx.pipeline().get(SslHandler.class) == null){
                //System.out.println("未加密...");
                ctx.write(new DefaultFileRegion(file.getChannel(),0,file.length()));
            }
            else {
                //System.out.println("加密...");
                ctx.write(new ChunkedNioFile(file.getChannel()));
            }

            // 聊天室首页加载完成，刷新发送缓冲区内容至客户端
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            // 如果没有请求keep-alive，则关闭channel
            if(!keepAlive){
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * 如果是100请求，按照HTTP1.1的规范响应 CONTINUE 继续状态
     *
     * @param ctx
     */
    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    /**
     * 如果发生异常，关闭channel
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
