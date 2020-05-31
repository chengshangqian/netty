package com.fandou.learning.netty.kaikeba.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

/**
 * 演示发送端拆包：
 * 服务端作为接收方，直接将接收到的Frame解码为String后进行显示，不对这些Frame进行粘包与拆包。
 */
public class UnpackingServer {
    private NioEventLoopGroup parentGroup;
    private NioEventLoopGroup childGroup;
    private ServerBootstrap bootstrap;

    private int port;

    public UnpackingServer(){
        this(8080);
    }

    public UnpackingServer(int port){
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

                            // 字符串解码：仅解码不会对数据（实际是解码后的ByteBuf对象）进行拆包粘包
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
        // 因为示例没有使用拆包粘包的解码器，所有这里对应的数量将会是收到的数据帧或数据包的数量
        private int counter;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            // 对接收的数据帧Frame不做额外的编码解码处理或拆包粘包处理
            // 打印的结果显示，发送的数据5021字节被拆分为2个数据帧，即服务端接收到了两个数据包
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
        UnpackingServer server = new UnpackingServer(8088);
        server.run();
    }
}
