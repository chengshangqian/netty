package com.fandou.learning.netty.core.chapter2;

import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步I/O服务器
 */
public class AIOServer {
    /**
     * 服务器绑定的端口号
     */
    private final int port;

    /**
     * 初始化端口号
     *
     * @param port
     */
    public AIOServer(int port) {
        this.port = port;
    }

    /**
     * 启动服务器：绑定并监听端口号
     */
    public void listen(){
        try {
            // 创建可缓存的线程池
            ExecutorService executorService = Executors.newCachedThreadPool();

            // 使用线程池创建异步channel组：初始化1个线程
            AsynchronousChannelGroup threadGroup = AsynchronousChannelGroup.withCachedThreadPool(executorService, 1);

            // 服务器主线程channel
            final AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(threadGroup);

            // 绑定监听的端口：服务器启动
            server.bind(new InetSocketAddress(port));
            System.out.println("AIO服务器启动成功,正在监听端口" + port + "...");

            // 接收请求：并实现处理请求的CompletionHandler接口，AIOServer可以实现CompletionHandler接口（相当于回调），然后传入this即可
            server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

                // 创建缓冲区：创建的是直接缓冲区DirectByteBuffer实例，即操作系统空间/内核空间，或操作系统内存，拷贝复制可以减少一次（零拷贝），但创建较为消耗资源
                // 也可以用ByteBuffer.allocate()创建的堆缓冲区，看业务需求
                final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

                /**
                 * 成功接收到请求时触发此方法：以处理请求
                 *
                 * @param channel 客户端请求channel
                 * @param attachment 附件?
                 */
                @Override
                public void completed(AsynchronousSocketChannel channel, Object attachment) {

                    System.out.println("I/O操作成功，开始获取数据.");

                    try{
                        // 清空缓冲区，准备接收来自客户端的数据
                        buffer.clear();

                        // 从客户端channel中读取数据到缓冲区
                        channel.read(buffer).get();

                        // 从直接缓冲区读取数据：
                        // 直接缓冲区实例DirectByteBuffer不存储数据即byte数组，仅保存操作系统原生数组的引用，
                        // 所以不可以直接堆原生数据进行读取操作，比如buffer.array()将报不支持的操作异常
                        // 调用flip()方法后，接下来将从缓冲区头部开始读取数据[0-limit]字节
                        buffer.flip();
                        byte[] dst = new byte[buffer.limit()];
                        buffer.get(dst);
                        System.out.println("接收到来自客户端的数据:" + new String(dst, CharsetUtil.UTF_8));
                        // 读取后，复原buffer的为读取前的状态，以便接下来原样发送回去给客户端
                        //buffer.rewind();
                        buffer.flip();

                        // 响应客户端:将客户端发来的数据原样发送回去
                        channel.write(buffer);
                        buffer.flip();
                    } catch (Exception e) {
                        System.err.println("获取数据出错:");
                        e.printStackTrace();
                    } finally {
                        try {
                            // 关闭channel
                            channel.close();

                            // 继续监听
                            server.accept(null,this);
                        } catch (Exception e) {
                            System.err.println("关闭channel时出错:");
                            e.printStackTrace();
                        }
                    }
                }

                /**
                 * 接收客户端请求发生异常时触发此方法
                 *
                 * @param exc
                 * @param attachment
                 */
                @Override
                public void failed(Throwable exc, Object attachment) {
                    System.err.println("I/O操作(接收客户端请求)失败.");
                }
            });

            try {
                // 睡眠主线程
                System.out.println("主线程开始睡眠...");
                Thread.sleep(Integer.MAX_VALUE);
                System.out.println("主线程睡眠...");
            } catch (InterruptedException e) {
                System.err.println("睡眠期间被中断：");
                e.printStackTrace();
            }

        } catch (IOException ex){
            System.err.println("AIO服务器启动出错:");
            ex.printStackTrace();
        }
    }
}
