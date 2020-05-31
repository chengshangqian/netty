package com.fandou.learning.netty.kaikeba.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

/**
 * 演示发送端拆包：服务端作为接收方，直接将接收到的 Frame 解码为 String 后进行显示，不对这些 Frame
 * 进行粘包与拆包。
 */
public class DefEncoderServer {
    private NioEventLoopGroup parentGroup;
    private NioEventLoopGroup childGroup;
    private ServerBootstrap bootstrap;

    private int port;

    public DefEncoderServer(){
        this(8080);
    }

    public DefEncoderServer(int port){
        this.port = port;
    }

    public void run() throws InterruptedException {
        parentGroup = new NioEventLoopGroup();
        childGroup = new NioEventLoopGroup();

        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(parentGroup, childGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline =  ch.pipeline();

                            // 字符串解码
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

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            // 对接收的数据包(帧Frame)不做额外的编码解码处理，字节打印，观察和发送方原始发送的信息的差异:
            // 收到了2个数据，但第1个数据的长度和内容与客户端发送的第一个数据长度和内容不一致，第2个数据也与客户端发送的第二个数据的长度和内容不一致
            // 收到的第1个数据长度较客户端的短，即第1个数据的内容被客户端发送前拆包了，第2个数据长度较客户端的长，包含了第1个数据被拆分的剩余部分粘合，被粘包发送
            System.out.println("收到的第【" + ++counter + "】个数据："  + msg);
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
        DefEncoderServer server = new DefEncoderServer(8088);
        server.run();
    }
}
