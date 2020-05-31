package com.fandou.learning.netty.core.chapter2;

import org.junit.jupiter.api.Test;

class AIOServerTest {
    @Test
    void testAIOServer(){
        AIOServer server = new AIOServer(12345);
        server.listen();
    }
}