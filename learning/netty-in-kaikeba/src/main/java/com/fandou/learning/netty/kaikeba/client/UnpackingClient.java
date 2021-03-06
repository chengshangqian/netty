package com.fandou.learning.netty.kaikeba.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 演示发送端拆包：
 * 客户端向服务端发送一个足够大的 ByteBuf 数据包（或多个足够大的数据包），两个数据包会被拆分为若干个数据帧Frame后通过TCP协议进行发送。
 * 这个过程中会发生拆包与粘包。
 */
public class UnpackingClient {
    private NioEventLoopGroup group;
    private Bootstrap bootstrap;

    private String serverHost;
    private int serverPort;

    public UnpackingClient(String serverHost, int serverPort){
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void run() throws InterruptedException {
        group = new NioEventLoopGroup();
        try{
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 字符串编码
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

                            // 自定义处理器
                            pipeline.addLast(new DefHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(this.serverHost,this.serverPort).sync();
            System.out.println("客户端启动...");

            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * 处理器
     */
    private class DefHandler extends ChannelInboundHandlerAdapter {
        // 发送的消息：数据量比较大，5021字节
        private String message = "Netty is a NIO client server framework " +
                "which enables quick and easy development of network applications " +
                "such as protocol servers and clients. It greatly simplifies and " +
                "streamlines network programming such as TCP and UDP socket server." +
                "'Quick and easy' doesn't mean that a resulting application will " +
                "suffer from a maintainability or a performance issue. Netty has " +
                "this guide and play with Netty.In other words, Netty is an NIO " +
                "framework that enables quick and easy development of network " +
                "as protocol servers and clients. It greatly simplifies and network " +
                "programming such as TCP and UDP socket server development.'Quick " +
                "not mean that a resulting application will suffer from a maintain" +
                "performance issue. Netty has been designed carefully with the expe " +
                "from the implementation of a lot of protocols such as FTP, SMTP, " +
                " binary and text-based legacy protocols. As a result, Netty has " +
                "a way to achieve of development, performance, stability, without " +
                "which enables quick and easy development of network applications " +
                "such as protocol servers and clients. It greatly simplifies and " +
                "streamlines network programming such as TCP and UDP socket server." +
                "'Quick and easy' doesn't mean that a resulting application will " +
                "suffer from a maintainability or a performance issue. Netty has " +
                "this guide and play with Netty.In other words, Netty is an NIO " +
                "framework that enables quick and easy development of network " +
                "as protocol servers and clients. It greatly simplifies and network " +
                "programming such as TCP and UDP socket server development.'Quick " +
                "not mean that a resulting application will suffer from a maintain" +
                "performance issue. Netty has been designed carefully with the expe " +
                "from the implementation of a lot of protocols such as FTP, SMTP, " +
                " binary and text-based legacy protocols. As a result, Netty has " +
                "a way to achieve of development, performance, stability, without " +
                "which enables quick and easy development of network applications " +
                "such as protocol servers and clients. It greatly simplifies and " +
                "streamlines network programming such as TCP and UDP socket server." +
                "'Quick and easy' doesn't mean that a resulting application will " +
                "suffer from a maintainability or a performance issue. Netty has " +
                "this guide and play with Netty.In other words, Netty is an NIO " +
                "framework that enables quick and easy development of network " +
                "as protocol servers and clients. It greatly simplifies and network " +
                "programming such as TCP and UDP socket server development.'Quick " +
                "not mean that a resulting application will suffer from a maintain" +
                "performance issue. Netty has been designed carefully with the expe " +
                "from the implementation of a lot of protocols such as FTP, SMTP, " +
                " binary and text-based legacy protocols. As a result, Netty has " +
                "a way to achieve of development, performance, stability, without " +
                "which enables quick and easy development of network applications " +
                "such as protocol servers and clients. It greatly simplifies and " +
                "framework that enables quick and easy development of network " +
                "as protocol servers and clients. It greatly simplifies and network " +
                "programming such as TCP and UDP socket server development.'Quick " +
                "not mean that a resulting application will suffer from a maintain" +
                "performance issue. Netty has been designed carefully with the expe " +
                "from the implementation of a lot of protocols such as FTP, SMTP, " +
                " binary and text-based legacy protocols. As a result, Netty has " +
                "a way to achieve of development, performance, stability, without " +
                "which enables quick and easy development of network applications " +
                "such as protocol servers and clients. It greatly simplifies and " +
                "framework that enables quick and easy development of network " +
                "as protocol servers and clients. It greatly simplifies and network " +
                "programming such as TCP and UDP socket server development.'Quick " +
                "not mean that a resulting application will suffer from a maintain" +
                "performance issue. Netty has been designed carefully with the expe " +
                "from the implementation of a lot of protocols such as FTP, SMTP, " +
                " binary and text-based legacy protocols. As a result, Netty has " +
                "a way to achieve of development, performance, stability, without " +
                "which enables quick and easy development of network applications " +
                "such as protocol servers and clients. It greatly simplifies and " +
                "streamlines network programming such as TCP and UDP socket server." +
                "'Quick and easy' doesn't mean that a resulting application will " +
                "suffer from a maintainability or a performance issue. Netty has " +
                "this guide and play with Netty.In other words, Netty is an NIO " +
                "framework that enables quick and easy development of network " +
                "as protocol servers and clients. It greatly simplifies and network " +
                "programming such as TCP and UDP socket server development.'Quick " +
                "not mean that a resulting application will suffer from a maintain" +
                "performance issue. Netty has been designed carefully with the expe " +
                "from the implementation of a lot of protocols such as FTP, SMTP, " +
                " binary and text-based legacy protocols. As a result, Netty has " +
                "a way to achieve of development, performance, stability, without " +
                "a compromise.=====================================================";

        /**
         * 客户端启动后，发送两次message（也可以发送一次，只要发送的数据足够大）
         * 观察服务端接收到的数据格式和原来发送的数据差异
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("每个消息的大小 => " + message.getBytes(CharsetUtil.UTF_8).length);
            ctx.channel().writeAndFlush(message);
            //ctx.channel().writeAndFlush(message);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // 打印异常栈
            cause.printStackTrace();
            // 关闭channel
            ctx.close();
        }
    }

    /**
     * 启动客户端
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        UnpackingClient client = new UnpackingClient("localhost",8088);
        client.run();
    }
}
