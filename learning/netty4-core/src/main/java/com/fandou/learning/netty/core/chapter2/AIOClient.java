package com.fandou.learning.netty.core.chapter2;

import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * 异步I/O客户端
 */
public class AIOClient {

    /**
     * 客户端channel
     */
    private final AsynchronousSocketChannel client;

    /**
     * 初始化客户端channel
     *
     * @throws IOException
     */
    public AIOClient() throws IOException {
        client = AsynchronousSocketChannel.open();
    }

    /**
     * 连接AIO服务器
     *
     * @param host AIO服务器主机
     * @param port AIO服务器端口号
     */
   public void connect(String host,int port){

       // 连接AIO服务器，连接成功将发送问好消息：AIOClient也可以实现CompletionHandler接口（相当于回调），然后传入this即可
        client.connect(new InetSocketAddress(host, port), null, new CompletionHandler<Void, Object>() {

            /**
             * 连接AIO服务器成功时触发此方法：发送问好
             *
             * @param result
             * @param attachment
             */
            @Override
            public void completed(Void result, Object attachment) {
                try {
                    // 要发送的数据字节数据
                    byte[] msg = "你好，欢迎来到AIO的世界！".getBytes(CharsetUtil.UTF_8);

                    // 向服务器端发送消息，直到发送完成：get()会等待
                    client.write(ByteBuffer.wrap(msg)).get();

                    // 发送完成打印成功提示
                    System.out.println("发送" + msg.length + "字节数据给AIO服务器.");
                } catch (Exception e) {
                    System.err.println("发送数据给AIO服务器时出错:");
                    e.printStackTrace();
                }
            }

            /**
             * 连接AIO服务器失败时触发此方法
             *
             * @param exc
             * @param attachment
             */
            @Override
            public void failed(Throwable exc, Object attachment) {
                System.err.println("连接AIO服务器时出错:");
                exc.printStackTrace();
            }
        });

        // 创建缓冲区：创建的是堆缓冲区HeapByteBuffer实例，即JVM堆内存也即当前应用程序内存空间
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        // 读取服务器响应，如果有的话，打印到控制台
        client.read(buffer, null, new CompletionHandler<Integer, Object>() {
            /**
             * 缓冲区数据可读取时，触发此方法
             *
             * @param length 读取到的数据大小（字节数）
             * @param attachment
             */
            @Override
            public void completed(Integer length, Object attachment) {
                System.out.println("I/O操作完成:读取从AIO服务器反馈回来的数据为" + length + "字节");
                System.out.println("读取从AIO服务器反馈回来的数据内容: " + new String(buffer.array(),CharsetUtil.UTF_8));
            }

            /**
             * 缓冲区数据读取失败时，触发此方法
             * @param exc
             * @param attachment
             */
            @Override
            public void failed(Throwable exc, Object attachment) {
                System.err.println("读取从AIO服务器反馈回来的数据时出错:");
                exc.printStackTrace();
            }
        });
   }
}
