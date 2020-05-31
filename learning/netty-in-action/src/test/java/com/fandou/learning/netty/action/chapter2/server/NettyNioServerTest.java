package com.fandou.learning.netty.action.chapter2.server;

import org.junit.jupiter.api.Test;

class NettyNioServerTest {

    @Test
    void start() throws InterruptedException {
        new NettyNioServer(8088).start();
    }
}