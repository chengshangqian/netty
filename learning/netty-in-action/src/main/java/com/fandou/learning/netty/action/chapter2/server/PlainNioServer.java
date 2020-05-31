package com.fandou.learning.netty.action.chapter2.server;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import sun.nio.ch.SelectionKeyImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 使用NIO原生API的服务端
 */
public class PlainNioServer {
    private final int port;

    // 服务端要传输的消息
    private final ByteBuffer message = ByteBuffer.wrap("你好!".getBytes(CharsetUtil.UTF_8));

    public PlainNioServer(int port) {
        this.port = port;
    }

    public  void start () throws IOException {
        // 创建服务端的Channel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        // 设置为非阻塞即异步传输方式
        serverChannel.configureBlocking(false);

        // 创建ServerSocket，然后绑定指定的端口，开始监听端口的请求
        ServerSocket serverSocket = serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        serverSocket.bind(address);
        System.out.println("服务端启动成功.");

        // 打开Selector来处理Channel事件
        Selector selector = Selector.open();

        // 将serverChannel对应的serverSocket注册到Selector，以接收连接
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 开始遍历事件
        for (;;) {
            try {
                // 遍历是否有就绪的事件：一直阻塞直到有事件产生
                // 也可以使用非阻塞的selectNow或超时的select(timeout)方法，区别是后面代码写法有所不同
                selector.select();
            } catch ( IOException ex){
                System.err.println("监听事件时出现异常...");
                break;
            }

            Set<SelectionKey> readKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                try {
                    // 连接
                    if(key.isAcceptable()){
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
                        SocketChannel clientSocketChannel = serverSocketChannel.accept();
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ,message.duplicate());
                    }

                    // 可写：发送打招呼信息
                    if(key.isWritable()){
                        SocketChannel client = (SocketChannel)key.channel();
                        ByteBuffer buffer = (ByteBuffer)key.attachment();
                        while (buffer.hasRemaining()) {
                            if(client.write(buffer) == 0) {
                                break;
                            }
                        }
                        client.close();
                    }
                } catch (IOException err) {
                    System.err.println("程序运行出现异常...");
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (Exception ex){
                        System.err.println("程序运行出现异常,尝试关闭channel时出错...");
                    }
                }
            }
        }
    }
}
