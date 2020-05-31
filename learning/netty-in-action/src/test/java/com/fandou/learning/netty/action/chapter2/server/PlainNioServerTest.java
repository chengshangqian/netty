package com.fandou.learning.netty.action.chapter2.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class PlainNioServerTest {

    @Test
    void start() throws IOException {
        new PlainNioServer(8088).start();
    }
}