package com.fandou.learning.netty.core.chapter4.nio;

import com.fandou.learning.netty.core.chapter4.server.HttpServer;
import org.junit.jupiter.api.Test;

class NIOTomcatTest {

    @Test
    void testNIOTomcat(){
        HttpServer server = new NIOTomcat(8080);
        server.start();
    }
}