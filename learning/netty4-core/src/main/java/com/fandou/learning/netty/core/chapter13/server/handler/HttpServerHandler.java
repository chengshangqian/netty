package com.fandou.learning.netty.core.chapter13.server.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Http请求处理器
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * 日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpServerHandler.class);

    /**
     * 聊天室websocket的URI
     */
    private final String imUri;

    /**
     * 初始化基于WebSocket协议的聊天室的相对访问地址
     *
     * @param imUri
     */
    public HttpServerHandler(String imUri) {
        this.imUri = imUri;
        contentTypes.put(".html","text/html");
        contentTypes.put(".css","text/css");
        contentTypes.put(".js","text/javascript");
        contentTypes.put(".jpg","image/jpg");
        contentTypes.put(".png","image/png");
        contentTypes.put(".gif","image/gif");
    }

    /**
     * 项目根路径
     */
    private URL location = HttpServerHandler.class.getProtectionDomain().getCodeSource().getLocation();

    /**
     * contentType映射
     */
    private final Map<String,String> contentTypes = new HashMap<>();

    /**
     * 获取资源文件
     *
     * @param fileName
     * @return
     * @throws Exception
     */
    private File getResource(String fileName) throws Exception {
        try{
            String path = location.toURI()  + "webroot" + fileName;
            path = !path.contains("file:") ? path : path.substring(5);
            path = path.replaceAll("//","/");
            return new File(path);
        }
        catch (URISyntaxException ex){
            throw new IllegalStateException("无法加载资源文件",ex);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        if(imUri.equalsIgnoreCase(request.uri())){
        //if(request.uri().startsWith(imUri)){
            // 增加引用计数并将请求传递给下一个处理器（WebSocket帧的处理器）进行处理
            ctx.fireChannelRead(request.retain());
            return;
        }

        // http升级为websocket
        if(HttpUtil.is100ContinueExpected(request)){
            send100Continue(ctx);
        }

        /**
         * 聊天室首页
         */
        String uri = request.uri();
        String page = uri.equals("/") ? "/chat.html" : uri;
        String ext = page.substring(page.lastIndexOf(".")).toLowerCase().trim();

        // 当前请求的资源不在支持的类型中
        if(!contentTypes.containsKey(ext)){
            ctx.fireChannelRead(request.retain());
            return;
        }

        // 资源文件
        RandomAccessFile file = new RandomAccessFile(getResource(page),"r");

        // 创建响应
        HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);

        // 设置响应头中的内容类型和字符编码
        response.headers().set(HttpHeaderNames.CONTENT_TYPE,contentTypes.get(ext) + ";charset=utf-8");

        // 检查是否请求了keep-alive
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if(keepAlive){
            // 如果是keep-alive请求，设置对应的响应头信息：内容长度和连接类型
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH,file.length());
            response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
        }

        // 将响应写入给客户端
        ctx.write(response);

        // 将首页内容写到客户端
        if(ctx.pipeline().get(SslHandler.class) == null){
            ctx.write(new DefaultFileRegion(file.getChannel(),0,file.length()));
        }
        else {
            ctx.write(new ChunkedNioFile(file.getChannel()));
        }

        // 资源加载完成，刷新发送缓冲区内容至客户端
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        // 如果没有请求keep-alive，则关闭channel
        if(!keepAlive){
            future.addListener(ChannelFutureListener.CLOSE);
        }

        // 关闭资源文件
        file.close();
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info(ctx.channel().remoteAddress() + "发生异常 -> ");
        cause.printStackTrace();
        ctx.close();
    }
}
