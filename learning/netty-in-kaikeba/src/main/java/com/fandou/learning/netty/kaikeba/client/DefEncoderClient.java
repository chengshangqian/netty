package com.fandou.learning.netty.kaikeba.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 演示自定义编码和解码器：演示对应服务端DefEncoderServer
 */
public class DefEncoderClient {
    private NioEventLoopGroup group;
    private Bootstrap bootstrap;

    private String serverHost;
    private int serverPort;

    public DefEncoderClient(String serverHost, int serverPort){
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

                            // 发送的时候自定义编码，自己定义Bytebuf大小，观察发送和接收数据的差异
                           // pipeline.addLast(new StringEncoder());

                            // 自定义处理器:发送消息的时候使用自定义编码(只定义使用的ByteBuf的大小和每次发送的信息大小一致，以验证观察接收端是否收到时也一致或进行了拆包粘包)
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

        // 发送的消息：数据量比较大
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
         * 客户端启动后，发送自定义编码后的message
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 消息数据
            byte[] data = message.getBytes("UTF-8");

            // 缓存空间
            ByteBuf buffer = null;

            // 发送2次
            for (int i = 0; i < 2; i++) {
                // 申请缓存空间：大小与发送的数据大小一致，以观察接收方收到数据时是否按照这个大小打印出来或进行了拆包粘包
                buffer = Unpooled.buffer(data.length);

                // 将发送的数据即字符串消息写入到缓存
                buffer.writeBytes(data);

                // 将缓存中的数据写入到Channel
                ctx.channel().writeAndFlush(buffer);
            }
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
        DefEncoderClient client = new DefEncoderClient("localhost",8088);
        client.run();
    }
}
