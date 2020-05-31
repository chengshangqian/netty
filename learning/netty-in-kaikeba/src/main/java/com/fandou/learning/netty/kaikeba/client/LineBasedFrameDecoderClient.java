package com.fandou.learning.netty.kaikeba.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 按照行的帧解码器演示：发送的数据带换行符，即以换行符作为分割数据，然后将分割的数据封装为ByteBuf
 * 观察行解码器的解码应用
 */
public class LineBasedFrameDecoderClient {
    private NioEventLoopGroup group;
    private Bootstrap bootstrap;

    private String serverHost;
    private int serverPort;

    public LineBasedFrameDecoderClient(String serverHost, int serverPort){
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
        private String separator = System.getProperty("line.separator"); // 定义分隔符：使用换行符作为分隔符

        // 发送的消息：数据量比较大，添加了3个行分割符，大小5027字节
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
                separator + // 添加第1个换行符
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
                separator + // 添加第2个换行符
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
                "a compromise.=====================================================" +
                separator; // 添加第3个换行符，服务端使用行解码器进行解码

        /**
         * 客户端启动后，发送message
         * 观察服务端接收到的数据格式和原来发送的数据差异
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 服务端按照行解析时，定义的解码器的大小需要大于这个长度
            System.out.println("每个消息的大小 => " + message.getBytes(CharsetUtil.UTF_8).length);
            ctx.channel().writeAndFlush(message);
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
        LineBasedFrameDecoderClient client = new LineBasedFrameDecoderClient("localhost",8088);
        client.run();
    }
}
