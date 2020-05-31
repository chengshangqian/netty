package com.fandou.learning.netty.action.chapter2.server;

import org.junit.jupiter.api.Test;

class EchoServerTest {

    @Test
    void start() throws InterruptedException {
        // 创建并启动Echo服务器
        new EchoServer(8088).start();
    }
}