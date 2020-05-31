package com.fandou.learning.netty.action.chapter2.server;

import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 传统的阻塞IO方式的服务端
 */
public class PlainOioServer {
    private final int port;

    private final byte[] message = "你好!".getBytes(CharsetUtil.UTF_8);

    public PlainOioServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(port);
        try{
            for (;;) {
                // 监听连接，如果没有连接，将一直阻塞
                final Socket clientSocket = serverSocket.accept();
                //System.out.println("接收到新的客户端连接,下面创建一个新的线程处理客户端的请求...");

                // 创建一个新的线程来处理来自客户端的请求
                new Thread(new Runnable() {

                    /**
                     * 处理请求的业务逻辑代码
                     */
                    @Override
                    public void run() {
                        // 输出流
                        OutputStream out;

                        try {
                            // 获取客户端连接中的输出流
                            out = clientSocket.getOutputStream();

                            // 处理业务逻辑，响应客户端请求：打声招呼，你好!
                            out.write(message);
                            out.flush();
                        } catch (IOException ex) {
                            System.err.println("响应客户端请求时出现异常...");
                        } finally {
                            try {
                                // 关闭连接
                                clientSocket.close();
                            } catch (IOException e) {
                                System.err.println("关闭客户端连接时出现异常...");
                            }
                        }
                    }
                }).start(); // 启动线程
            }
        } catch (IOException exception) {
            System.err.println("程序运行出现异常...");
        }
    }
}
