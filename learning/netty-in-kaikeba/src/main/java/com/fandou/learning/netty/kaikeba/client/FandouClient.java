package com.fandou.learning.netty.kaikeba.client;

import com.fandou.learning.netty.kaikeba.server.FandouBean;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

/**
 * 演示自定义编/解码器
 */
public class FandouClient {

    private String serverHost;
    private int serverPort;

    public FandouClient(String serverHost, int serverPort){
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void run() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 添加自定义对象的编码器
                            pipeline.addLast(new FandouEncoder());

                            // 字符串编/解码
                            // pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

                            // 自定义处理器：收发消息
                            pipeline.addLast(new DefHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(serverHost,serverPort).sync();
            System.out.println("客户端启动...");

            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * 处理器
     */
    private class DefHandler extends SimpleChannelInboundHandler<String> {
        // 收到的数据包数量
        private int counter;

        /**
         * 客户端启动后，发送message
         * 观察服务端接收到的数据格式和原来发送的数据差异
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            String service = "https://api.fandou.com/v1";
            byte application = (byte)0xA;
            byte category = (byte)0xB;
            int length = service.getBytes(CharsetUtil.UTF_8).length;

            // 创建一个自定义的bean
            FandouBean bean = new FandouBean(application,category,length,service);

            // 发送给服务器：发送前将会被编码器FandouEncoder编码后再传送
            ctx.channel().writeAndFlush(bean);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println("收到服务端响应【" + ++counter + "】 => " + msg);

            // 测试：来回发送
            String service = "https://api.fandou.com/v1";
            byte application = (byte)0xA;
            byte category = (byte)0xB;
            int length = service.getBytes(CharsetUtil.UTF_8).length;

            // 创建一个自定义的bean
            FandouBean bean = new FandouBean(application,category,length,service);

            // 发送给服务器：发送前将会被编码器FandouEncoder编码后再传送
            ctx.channel().writeAndFlush(bean);
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
