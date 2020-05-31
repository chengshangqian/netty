package com.fandou.learning.netty.action.chapter2.server;

import org.junit.jupiter.api.Test;

class NettyOioServerTest {

    @Test
    void start() throws InterruptedException {
        new NettyOioServer(8088).start();
    }
}