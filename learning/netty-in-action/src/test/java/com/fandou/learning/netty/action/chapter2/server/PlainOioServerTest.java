package com.fandou.learning.netty.action.chapter2.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class PlainOioServerTest {

    @Test
    void start() throws IOException {
        new PlainOioServer(8088).start();
    }
}