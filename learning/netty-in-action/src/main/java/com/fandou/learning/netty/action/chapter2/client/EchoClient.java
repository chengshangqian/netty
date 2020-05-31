package com.fandou.learning.netty.action.chapter2.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * echo客户端
 */
public class EchoClient {
    // 服务器端主机地址
    final String host;

    // 服务器端主机端口
    final int port;

    public EchoClient(String host,int port){
        this.host = host;
        this.port  = port;
    }

    /**
     * 启动echo客户端
     */
    public void start() throws InterruptedException {
        // 创建echo客户端处理器
        final EchoClientHandler echoClientHandler = new EchoClientHandler();

        // 创建事件循环组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // 创建客户端引导
            Bootstrap bootstrap = new Bootstrap();

            // 指定使用的事件循环组
            bootstrap.group(group)

                    // 指定创建用于NIO传输的Channel类型：客户端使用NioSocketChannel
                    .channel(NioSocketChannel.class)

                    // 指定echo服务器端地址
                    .remoteAddress(new InetSocketAddress(host,port))

                    // 添加echoClientHandler到ChannelPipeline中
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(echoClientHandler);
                        }
                    });

            // 连接服务器，调用sync()方法阻塞直到连接成功
            ChannelFuture future = bootstrap.connect().sync();
            System.out.println("客户端启动成功.");

            // 获取Channel的CloseFuture，并且阻塞当前线程直到它完成
            future.channel().closeFuture().sync();
            System.out.println("关闭Channel.");
        } finally {
            // 优雅关闭group
            System.out.println("优雅关闭group.");
            group.shutdownGracefully();
        }
    }

    /**
     * 定义客户端处理器
     * 继承SimpleChannelInboundHandler，实现的channelRead0中的消息在执行完channelRead0后将会被释放
     * 标记为Sharable，可以被多个channel安全地共享
     */
    @ChannelHandler.Sharable
    private class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        /**
         * 当连接建立被激活时触发此方法
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // channel激活可用/活跃之后，立即发送消息
            ctx.channel().writeAndFlush(Unpooled.copiedBuffer("Hello,Netty!",CharsetUtil.UTF_8));
        }

        /**
         * 读取到可用数据包（ByteBuf）时将触发此方法：此方法调用完毕后，msg将会被GC回收
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            System.out.println("收到来自服务端的消息 => " + msg.toString(CharsetUtil.UTF_8));
        }

        /**
         * 发生异常时触发此方法
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
