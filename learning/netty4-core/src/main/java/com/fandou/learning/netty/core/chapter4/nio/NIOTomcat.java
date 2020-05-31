package com.fandou.learning.netty.core.chapter4.nio;

import com.fandou.learning.netty.core.chapter4.server.AbstractHttpServer;
import com.fandou.learning.netty.core.chapter4.server.Request;
import com.fandou.learning.netty.core.chapter4.server.Response;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Netty实现的非阻塞IO的Http服务器
 */
public class NIOTomcat extends AbstractHttpServer {

    /**
     * 初始化
     *
     * @param port 监听的端口号
     */
    public NIOTomcat(int port) {
       super(port);
    }

    /**
     * 启动服务端，监听客户端请求
     */
    @Override
    public void doStart() {
        /*** Netty事件驱动异步IO的实现 ***/
        // 专门用于接收HTTP请求的事件循环组即线程或线程组，主线程：接收请求，然后将请求转交给处理HTTP请求线程或线程组进行处理
        EventLoopGroup acceptRequestGroup = new NioEventLoopGroup();

        // 专门用于处理HTTP请求的事件循环组即线程或线程组，子线程：处理具体的业务逻辑
        EventLoopGroup handleRequestGroup = new NioEventLoopGroup();

        try{
            // 创建服务端引导
            ServerBootstrap server = new ServerBootstrap();

            // 初始化服务端引导
            server.group(acceptRequestGroup,handleRequestGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // Netty提供的HTTP请求编码器
                            pipeline.addLast(new HttpServerCodec());

                            // 添加客户端请求处理器
                            pipeline.addLast(new NIOHttpRequestHandler());
                        }
                    })
                    // 接受请求的主线程选线：分配线程最大数量
                    .option(ChannelOption.SO_BACKLOG,128)
                    // 处理请求的子线程选项：保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE,true);

            // 绑定监听端口号，启动服务器，开始监听客户端的请求
            ChannelFuture future = server.bind(getPort()).sync();
            System.out.println("NIOTomcat启动,监听端口" + getPort());
            future.channel().closeFuture().sync();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        finally {
            // 关闭线程池
            acceptRequestGroup.shutdownGracefully();
            handleRequestGroup.shutdownGracefully();
        }
    }

    /**
     * HTTP请求处理器
     */
    private class NIOHttpRequestHandler extends ChannelInboundHandlerAdapter {

        /**
         * 当管道缓冲区中有可以读的客户开端请求内容时触发此方法
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof HttpRequest){
                // Netty封装的HTTP请求信息
                HttpRequest req = (HttpRequest) msg;

                // 创建自定义的HTTP请求和响应
                Request request = new NIOHttpRequest(ctx,req);
                Response response = new NIOHttpResponse(ctx,req);

                // 打印请求信息
                System.out.println("request => " + request);

                // 获取请求的资源路径即url
                String url = request.getUrl();

                if(null != url){
                    // 调用对应的servlet处理响应客户端的请求
                    if(getServletMapping().containsKey(url)){
                        getServletMapping().get(url).service(request,response);
                    }
                    // 如果请求的资源即url不存在，返回404
                    else  if("/favicon.ico".equalsIgnoreCase(url)){
                        // TODO
                        response.write("404 - Not Found.");
                    }
                    else {
                        response.write("404 - Not Found.");
                    }
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.err.println("处理客户端请求发生异常:");
            cause.printStackTrace();
            ctx.close();
        }
    }
}
