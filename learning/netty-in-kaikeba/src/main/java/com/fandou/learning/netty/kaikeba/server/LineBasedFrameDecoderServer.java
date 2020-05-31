package com.fandou.learning.netty.kaikeba.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

/**
 * 演示行解码器的使用：将以换行符分隔接收到的数据，然后保存为ByteBuf
 */
public class LineBasedFrameDecoderServer {
    private NioEventLoopGroup parentGroup;
    private NioEventLoopGroup childGroup;
    private ServerBootstrap bootstrap;

    private int port;

    public LineBasedFrameDecoderServer(){
        this(8080);
    }

    public LineBasedFrameDecoderServer(int port){
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

                            // 使用行解码器：maxLength应大于客户端发送消息的最大值（单位字节）
                            // 注意和StringDecoder字符串解码器的顺序，LineBasedFrameDecoder需要放在StringDecoder之前，因为
                            // LineBasedFrameDecoder将数据帧(即二进制数据流或byte字节流）转换为ByteBuf
                            // 然后，StringDecoder则是将ByteBuf转换为字符串
                            // 看两者继承的父类名称：
                            // LineBasedFrameDecoder父类是ByteToMessageDecoder，即字节到【消息】的解码器
                            // StringDecoder的父类是MessageToMessageDecoder<ByteBuf>，即【消息】（ByteBuf类型）到消息(StringDecoder实现的是String类型)的解码器
                            // ByteToMessageDecoder属于一次解码器，即对接收到的数据进行的首次解码，得到一次解码对象
                            // MessageToMessageDecoder属于二次解码器，即将一次解码器解码的对象解析为其它类型的对象
                            pipeline.addLast(new LineBasedFrameDecoder(5120));

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
            // 对接收的数据帧先进行行解码，然后再转为字符串
            // 按照换行符将一条数据分割封装为3个ByteBuf对象(由StringDecoder解码为字符串显示)
            // 即LineBasedFrameDecoder将收到的2个字节流数据帧（参考发送端拆包UnpackingClient/UnpackingServer示例）按照换行符解码为3个ByteBuf对象
            // 然后StringDecoder将3个ByteBuf对象解码为String对象
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
        LineBasedFrameDecoderServer server = new LineBasedFrameDecoderServer(8088);
        server.run();
    }
}
