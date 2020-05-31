package com.fandou.learning.netty.action.chapter12;

import io.netty.channel.ChannelFuture;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

class ChatServerTest {

    @Test
    void testChatServer(){
        // 创建聊天室服务端
        final ChatServer endpoint = new ChatServer();

        // 绑定本地端口9999启动聊天室
        ChannelFuture future = endpoint.start(new InetSocketAddress(9999));

        // 线程关闭，清理资源
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                endpoint.destroy();
            }
        });

        // 关闭channel
        future.channel().closeFuture().syncUninterruptibly();
    }
}