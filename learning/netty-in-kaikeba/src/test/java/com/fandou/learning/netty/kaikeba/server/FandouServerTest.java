package com.fandou.learning.netty.kaikeba.server;

import org.junit.jupiter.api.Test;

class FandouServerTest {

    @Test
    void testRun() throws InterruptedException {
        FandouServer server = new FandouServer(8088);
        server.run();
    }
}