package com.fandou.learning.netty.kaikeba.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 演示自定义长度解码器的使用：解析收到客户端发来数据包中原始消息的内容，丢弃长度域信息；然后发送响应给客户端
 */
public class LengthFieldBasedFrameDecoderServer {
    private NioEventLoopGroup parentGroup;
    private NioEventLoopGroup childGroup;
    private ServerBootstrap bootstrap;

    private int port;

    public LengthFieldBasedFrameDecoderServer(){
        this(8080);
    }

    public LengthFieldBasedFrameDecoderServer(int port){
        this.port = port;
    }

    public void run() throws InterruptedException {
        parentGroup = new NioEventLoopGroup();
        childGroup = new NioEventLoopGroup();

        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(parentGroup, childGroup)
                    .channel(NioServerSocketChannel.class)
                    // 先按行解码，然后(再解码)转为字符串
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline =  ch.pipeline();

                            // 定义自定义长度编码器的长度域为4个字节，其值为消息大小不包含长度域本身大小
                            int lengthFieldLength = 4;
                            boolean lengthIncludesLengthFieldLength = false;
                            pipeline.addLast(new LengthFieldPrepender(lengthFieldLength,lengthIncludesLengthFieldLength));

                            // 定义自定义长度解码器
                            // 已经客户端发送的数据长度为20个数字字母字符串+2字节长度域，总长为22字节，数据包大小合计为22字节，长度域的值即消息大小包含了长度域大小
                            // lengthAdjustment = 数据包总长22 - lengthFieldOffset大小0 - lengthFieldLength长度域大小2 - lengthFieldLength长度域的值即22 = -2
                            // 表示收到的客户端数据帧最长应该为32字节，数据包中含有长度域且在数据的开头，其大小为2字节，记录的消息大小包含了长度域本身的大小
                            // 丢弃/跳过长度域数据，解析原始的消息传递给自定义的处理器DefHandler进行处理
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(32,0,2,-2,2));

                            // 字符串编/解码
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

                            // 自定义处理器
                            pipeline.addLast(new DefHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(this.port).sync();
            System.out.println("服务器已启动...");

            future.channel().closeFuture().sync();
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }

    /**
     * 处理器
     */
    private class DefHandler extends SimpleChannelInboundHandler<String> {
        // 收到的数据包（实际上是被netty按照自定义或使用netty提供的解码器解码后的对象）数量
        private int counter;

        // 响应给客户端的消息
        private String message = "你好";

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            // 正常应该收到 1234567890ABCDEFGHIJ
            System.out.println("收到的第【" + ++counter + "】个数据："  + msg);

            // 发送响应消息
            // 消息message：大小6字节
            // 长度域：4字节
            // 长度域的值：十六进制数0x0006为长度域中的值，表示0x0006消息长度为6字节，即不包含长度域大小
            // 实际发送的数据包内容如下
            /**
             *  +--------+-------+
             *  + 0x0006 | "你好" |
             *  +--------+-------+
             */
            System.out.println("响应给客户端的消息大小："  + message.getBytes(CharsetUtil.UTF_8).length);
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
     * 启动服务器
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        LengthFieldBasedFrameDecoderServer server = new LengthFieldBasedFrameDecoderServer(8088);
        server.run();
    }
}
