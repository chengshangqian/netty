package com.fandou.learning.netty.action.chapter2.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * echo服务器端
 */
public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {

        // 创建服务端业务逻辑处理器：用了final，需要在类定义声明类为@Sharable
        final EchoServerHandler echoServerHandler = new EchoServerHandler();

        // 创建服务端事件循环组:连接请求事件循环组（parentGroup，处理连接请求）和请求事件循环组（childGroup，处理业务请求）
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();

        try {
            // 创建服务器端引导
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            // 指定处理连接请求和业务请求的事件循环组
            serverBootstrap.group(parentGroup,childGroup)

                    // 指定创建用于NIO传输的Channel类型：服务端使用NioServerSocketChannel
                    .channel(NioServerSocketChannel.class)

                    // 指定服务端本地绑定的端口
                    .localAddress(port)

                    // 添加处理业务逻辑的处理器到ChannelPipeline中
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(echoServerHandler);
                        }

                    });

            // 异步绑定服务器：调用sync方法阻塞直到绑定完成才执行下一行
            ChannelFuture future = serverBootstrap.bind().sync();
            System.out.println("服务器启动成功.");

            // 获取Channel的CloseFuture，并且阻塞当前线程直到它完成
            future.channel().closeFuture().sync();
            System.out.println("关闭Channel.");
        } finally {
            System.out.println("优雅关闭parentGroup|childGroup.");
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
    private class EchoServerHandler extends ChannelInboundHandlerAdapter {

        /**
         * 读取到可用数据包（ByteBuf）将会触发次方法
         * 客户端发送的一条完整消息，有可能会被解析成多个数据包，所有可能会由多次调用，取决于数据编码/解码的协议
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf in = (ByteBuf)msg;

            // 打印从客户端发送来的消息
            System.out.println("收到来自客户端的消息 => " + in.toString(CharsetUtil.UTF_8));
            // 将收到的消息再转发回去给客户端。
            // 由于是异步操作，真正的发送操作可能在执行完代码退出方法时，还未完成，所以此时不能释放资源(msg)，
            // 否则GC回收msg，有可能导致后面发送的消息内容为空,
            // 所有需要继承ChannelInboundHandlerAdapter，此类在方法执行完后，不会释放资源，需要手动释放
            ctx.channel().write(in);

            System.out.println("channelRead方法执行完毕.");
        }

        /**
         * 最后一次数据包读取完成后触发
         * 客户端发送一次消息过来，服务端可能解析为几个数据包（若干个ByteBuf，取决于数据编码/解码的协议），
         * 当最后一个数据包被读取完成后(执行完channelRead方法返回时)触发此方法
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            // 发送然后刷新/清空缓冲区内容
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER)
                    // 添加一个关闭Channel的监听器：当发送然后刷新/清空缓冲区内容完成时，执行此关闭Channel的操作
                    .addListener(ChannelFutureListener.CLOSE);

            System.out.println("触发channelReadComplete方法执行.");
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
            cause.printStackTrace();
            ctx.close();
        }

    }
}
