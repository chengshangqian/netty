package com.fandou.learning.netty.kaikeba.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 演示自定义解码器的使用
 */
public class FandouServer {
    private final int MAX_FRAME_LENGTH = 1024;
    private final int LENGTH_FIELD_OFFSET = 2;
    private final int LENGTH_FIELD_LENGTH = 4;
    private final int LENGTH_ADJUSTMENT = 0;
    private final int INITIAL_BYTES_TO_STRIP = 0;

    private int port;

    public FandouServer(int port){
        this.port = port;
    }

    public void run() throws InterruptedException {
        NioEventLoopGroup parentGroup = new NioEventLoopGroup();
        NioEventLoopGroup childGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(parentGroup, childGroup)
                    .channel(NioServerSocketChannel.class)
                    // 先按行解码，然后(再解码)转为字符串
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline =  ch.pipeline();

                            // 添加自定义对象的解码器
                            pipeline.addLast(new FandouDecoder(MAX_FRAME_LENGTH,LENGTH_FIELD_OFFSET,LENGTH_FIELD_LENGTH,LENGTH_ADJUSTMENT,INITIAL_BYTES_TO_STRIP,false));

                            // 字符串编/解码
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            // pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

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
    private class DefHandler extends SimpleChannelInboundHandler<FandouBean> {

        // 收到的数据包数量
        private int counter;

        // 响应给客户端的消息
        private final String message = "你好";

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FandouBean bean) throws Exception {
            System.out.println("收到的第【" + ++counter + "】个对象数据："  + bean);

            // 发送字符串问候语：你好
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
}
