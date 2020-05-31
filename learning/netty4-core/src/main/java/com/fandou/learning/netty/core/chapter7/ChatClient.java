package com.fandou.learning.netty.core.chapter7;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty实现的聊天客户端
 */
public class ChatClient {

    /**
     * 加入聊天：连接聊天服务器
     *
     * @param host 聊天服务器主机地址
     * @param port 聊天服务器主机端口
     * @param nickname 客户端用户昵称
     * @return
     */
    public ChatClient connect(String host,int port,String nickname){

        /**
         * 创建事件循环组：即EventLoop的分组，可以看成是线程组或线程池
         *
         * 它主要功能
         * 1.初始化主线程执行器executor
         * 2.初始化子事件循环EventLoop或子事件执行器EventExecutor数组children（相当于线程池）
         * 3.创建子事件执行器的选择器Selector
         * 4.为子事件执行器创建监听器，监听执行状态（完成/终止）
         * 5.初始化只读子事件执行器集合readonlyChildren
         */
        EventLoopGroup group = new NioEventLoopGroup();

        try{
            /**
             * 创建聊天客户端引导
             *
             * 无参构造函数实例化的Bootstrap对象，其包括了一个缺省地址解析器组、地址解析器组、引导配置对象、空白的选项即属性对象。
             * 要正常使用Bootstrap连接远程服务器，至少还需要配置以下4个参数，包括
             * 事件循环组（类似线程池）EventLoopGroup、通道类型ChannelType、通道(事件)处理器ChannelHandler、远程主机SocketAddress等重要组件，
             * 其它可选配置项还有通道选项ChannelOption、自定义属性Attributes等信息
             */
            Bootstrap client = new Bootstrap();

            /**
             * 设置客户端引导参数
             */
            // 1.设置事件循环组
            client.group(group)
                    // 2.设置后续与远程主机连接时的通道类型：非阻塞IO通道
                    .channel(NioSocketChannel.class)
                    // 3.添加通道(事件)处理器：即如何收发聊天内容
                    .handler(new ChatClientInitializer())
                    //4.指定了远程主机字符串地址以及端口
                    //.remoteAddress(host, port)
                    // 设置通道的选项：保持长连接
                    .option(ChannelOption.SO_KEEPALIVE,true);

            /**
             * 同步连接聊天服务器
             * 此方法连接时会对之前配置的参数（1，2，3,4）进行一次验证，
             * 包括事件循环组group、通道工厂channelFactory、通道事件处理器handler、远程主机地址remoteAddress等
             */
            //ChannelFuture future = client.connect().sync();

            /**
             * 4.指定了远程主机字符串地址以及端口，然后同步连接聊天服务器，启动客户端
             *
             * 它主要功能
             * 1.检查之前配置的参数（1，2，3），包括事件循环组group、通道工厂channelFactory、通道事件处理器handler
             * 2.创建channel，过程中内部会实例化一个Java NIO的套接字通道SocketChannel以及ChannelPipeline
             * 3.初始化channel，将设置的通道处理器handler、通道选项options、通道属性attrs等设置到创建的通道实例channel
             * 4.接着将channel注册到EventLoop上（selectKey和套接字通道、EventLoop关联上）
             * 5.最后开始解析并连接远程主机，
             */
            ChannelFuture future = client.connect(host, port).sync();
            System.out.println("客户端启动...");

            /**
             * 同步关闭连接channel
             */
            future.channel().closeFuture().sync();
        }
        catch (InterruptedException ex){
            ex.printStackTrace();
        }
        finally {
            group.shutdownGracefully();
        }
        return this;
    }
}
