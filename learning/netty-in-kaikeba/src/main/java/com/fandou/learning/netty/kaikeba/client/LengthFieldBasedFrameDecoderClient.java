package com.fandou.learning.netty.kaikeba.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 演示分通用编/解码器LengthFieldPrepender/LengthFieldBasedFrameDecoder（也称自定义长度解码器）的使用：
 * 客户端使用自定义编码器或Netty提供的LengthFieldPrepender通用编码器对发送的数据进行编码后发送数据包
 * 服务端收到客户端发送端数据后，也发送使用LengthFieldPrepender通用编码器编码的响应信息返回客户端
 */
public class LengthFieldBasedFrameDecoderClient {
    private NioEventLoopGroup group;
    private Bootstrap bootstrap;

    private String serverHost;
    private int serverPort;

    public LengthFieldBasedFrameDecoderClient(String serverHost, int serverPort){
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

                            /*****  自定义长度编码器设置开始  *****/
                            // 长度域的大小，定义客户端发送消息的数据包中长度域的大小，用于记录本次发送的消息长度
                            int lengthFieldLength = 2;

                            // 长度域的值是否包含长度域的大小
                            // true表示包含，此时发送的数据包大小 = lengthFieldLength + 消息内容的字节长度。默认false。
                            boolean lengthIncludesLengthFieldLength = true;

                            // 以上参数表示发送给服务端的数据包结构：
                            // LengthFieldPrepender表示发送的数据包包含长度域且在开头首位；
                            // 数据包中长度域部分大小为2字节；
                            // 发送的数据包中长度域的值包含了长度域的大小
                            pipeline.addLast(new LengthFieldPrepender(lengthFieldLength,lengthIncludesLengthFieldLength));
                            /*****  自定义长度编码器设置结束  *****/

                            /*****  自定义长度解码器设置开始  *****/
                            // 对方发送/我方接收数据包最大长度，如果不清楚或不确定，使用Integer.MAX_VALUE
                            int maxFrameLength = 1024;

                            // 长度域偏移量，即在发送的字节数组即数据中的下标，即开始位置，约定在首位开始，0即表示从第一个字节开始算起
                            int lengthFieldOffset = 0;

                            // 长度域大小：这里指服务端响应回来的数据包中，长度域的大小，与服务端一致才能正确解码，单位字节
                            int serverLengthFieldLength = 4;

                            // 长度校验值lengthAdjustment（长度域的偏移量矫正）:
                            // 如果长度域的值，除了包含有效有效数据/原始信息的长度外，还包含了其它域（比如长度域本身，自定义的头部等）的长度，则需要要进行矫正，
                            // 两种情况都满足公式：lengthAdjustment = 数据包总大小 - lengthFieldOffset - 长度域lengthFieldLength（serverLengthFieldLength） - 长度域lengthFieldLength的值
                            int lengthAdjustment = 0;

                            // 解析数据(转为ByteBuf)时跳过数据的大小
                            // 很多时候只需要业务数据即原始的消息，其它的头部或长度域仅做位解码或流程控制使用，可以不需要解析出来传递给下一个业务程序
                            int initialBytesToStrip = 4;

                            // 以上参数表示对接收到的服务端数据包解码时：
                            // 服务端的一个数据包构成应该是长度最到位1024字节；
                            // 每个数据包的长度域从首字节开始其长度位4，其记录的值表示的是原始消息的长度即不包含长度域的大小；
                            // 解析时跳过4个字节的数据，即跳过长度域，直接返回原始消息的内容
                            // lengthFieldOffset为0，表示消息头中长度域开头
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(maxFrameLength,lengthFieldOffset,serverLengthFieldLength,lengthAdjustment,initialBytesToStrip));
                            /*****  自定义长度解码器设置结束  *****/

                            // 字符串编/解码
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

                            // 自定义处理器：收发消息
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
    private class DefHandler extends SimpleChannelInboundHandler<String> {
        // 发送的消息：长度为20的数字字母字符串
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
            System.out.println("发送消息的大小 => " + message.getBytes(CharsetUtil.UTF_8).length);
            // 消息message：大小20字节
            // 长度域：2字节
            // 长度域的值：十六进制数0x0016为长度域中的值，表示0x0016消息长度为22字节，即包含长度域大小
            // 实际发送的数据包内容如下
            /**
             *  +--------+------------------------+
             *  + 0x0016 | "1234567890ABCDEFGHIJ" |
             *  +--------+------------------------+
             */
            ctx.channel().writeAndFlush(message);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            // 正常应该收到6字节：你好
            System.out.println("收到消息的大小 => " + msg.getBytes(CharsetUtil.UTF_8).length);
            System.out.println("收到的消息 => " + msg);
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
        LengthFieldBasedFrameDecoderClient client = new LengthFieldBasedFrameDecoderClient("localhost",8088);
        client.run();
    }
}
