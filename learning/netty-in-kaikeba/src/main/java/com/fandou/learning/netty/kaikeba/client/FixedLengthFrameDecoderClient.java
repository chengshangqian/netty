package com.fandou.learning.netty.kaikeba.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 演示分隔符解码器的使用：
 * 按照固定长度对接收的数据帧后进行分隔（不需要理会TCP数据帧的拆包粘包），分隔的数据封装为ByteBuf
 * 可以按照固定长度分割一个或多个数据帧，如果分割剩余的数据小于定义的长度，实测的结果时该剩余的数据将被丢弃
 */
public class FixedLengthFrameDecoderClient {
    private NioEventLoopGroup group;
    private Bootstrap bootstrap;

    private String serverHost;
    private int serverPort;

    public FixedLengthFrameDecoderClient(String serverHost, int serverPort){
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
        // 发送的消息：20个数字,正常会以一个数据帧发送并接收封装为一个ByteBuf。如果使用了固定长度解码器，将按照长度进行分隔解码。
        // 如果服务端以10长度分隔，将会是封装为2个ByteBuf；如果以5长度分隔，将会封装为4个ByteBuf；如果以4长度分隔，将会分装为5个ByteBuf
        private String message = "1234567890ABCDEFGHIJ";

        /**
         * 客户端启动后，发送message
         * 观察服务端接收到的数据格式和原来发送的数据差异
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("每个消息的大小 => " + message.getBytes(CharsetUtil.UTF_8).length);
            ctx.channel().writeAndFlush(message);

            // 发送定长消息，服务端按照定长解码，不需要理会TCP发送消息时的拆包粘包，非常方便
            for (int i = 0; i < 10; i++) {
                ctx.channel().writeAndFlush(message);
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
        FixedLengthFrameDecoderClient client = new FixedLengthFrameDecoderClient("localhost",8088);
        client.run();
    }
}
