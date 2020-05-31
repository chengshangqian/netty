package com.fandou.learning.netty.core.chapter5.rpc.registry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * 注册中心：接收服务提供者注册服务
 */
public class SimpleRegistry {
    private final int port;

    public SimpleRegistry(int port) {
        this.port = port;
    }

    public void start(){
        EventLoopGroup masterGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap server = new ServerBootstrap();

            server.group(masterGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // Netty提供的HTTP请求编码器
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            pipeline.addLast("encoder",new ObjectEncoder());
                            pipeline.addLast("decoder",new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));

                            pipeline.addLast(new RegistryHandler());
                        }
                    })
                    // 接受请求的主线程选线：分配线程最大数量
                    .option(ChannelOption.SO_BACKLOG,128)
                    // 处理请求的子线程选项：保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE,true);

            ChannelFuture future = server.bind(port).sync();
            System.out.println("SimpleRegistry启动,监听端口" + port);
            future.channel().closeFuture().sync();
        }
        catch (Exception ex){
            masterGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
