package com.fandou.learning.netty.core.chapter2;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class AIOClientTest {

    @Test
    void testAIOClient() throws IOException {
        AIOClient client = new AIOClient();
        client.connect("127.0.0.1",12345);
        System.out.println("===> OK");
    }
}