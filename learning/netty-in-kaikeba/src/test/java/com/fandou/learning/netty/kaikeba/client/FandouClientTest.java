package com.fandou.learning.netty.kaikeba.client;

import org.junit.jupiter.api.Test;

class FandouClientTest {

    @Test
    void testRun() throws InterruptedException {
        FandouClient client = new FandouClient("localhost",8088);
        client.run();
    }
}