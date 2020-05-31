package com.fandou.learning.netty.action.chapter2.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * 使用Netty框架实现的阻塞IO服务器端
 */
public class NettyOioServer {
    private final int port;

    // 要发送的消息
    private final ByteBuf message = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("你好!",CharsetUtil.UTF_8));

    public NettyOioServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {

        // 创建服务端业务逻辑处理器：用了final，需要在类定义声明类为@Sharable
        final DefServerHandler serverHandler = new DefServerHandler();

        // 创建服务端事件循环组:连接请求事件循环组（parentGroup，处理连接请求）和请求事件循环组（childGroup，处理业务请求）
        EventLoopGroup parentGroup = new OioEventLoopGroup();
        EventLoopGroup childGroup = new OioEventLoopGroup();

        try {
            // 创建服务器端引导
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            // 指定处理连接请求和业务请求的事件循环组
            serverBootstrap.group(parentGroup,childGroup)

                    // 指定创建用于OIO传输的Channel类型：服务端使用OioServerSocketChannel
                    .channel(OioServerSocketChannel.class)

                    // 指定服务端本地绑定的端口
                    .localAddress(port)

                    // 添加处理业务逻辑的处理器到ChannelPipeline中
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(serverHandler);
                        }

                    });

            // 异步绑定服务器：调用sync方法阻塞直到绑定完成才执行下一行
            ChannelFuture future = serverBootstrap.bind().sync();
            System.out.println("服务器启动成功.");

            // 获取Channel的CloseFuture，并且阻塞当前线程直到它完成
            future.channel().closeFuture().sync();
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }

    }

    /**
     * 继承ChannelInboundHandlerAdapter类。此类不会自动释放资源，需要手动释放。
     * 服务端收到消息后，再将消息重新发送给客户端
     * 标记为Sharable，可以被多个channel安全地共享
     */
    @ChannelHandler.Sharable
    private class DefServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.channel().writeAndFlush(message.duplicate()).addListener(ChannelFutureListener.CLOSE);
        }

        /**
         * 读取入站数据发生异常时触发此方法
         *
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            System.err.println("读取客户端数据时出现异常...");
            ctx.close();
        }

    }
}
