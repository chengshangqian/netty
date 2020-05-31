package com.fandou.learning.netty.core.chapter4.bio;

import com.fandou.learning.netty.core.chapter4.server.HttpServer;
import org.junit.jupiter.api.Test;

class BIOTomcatTest {
    @Test
    void testBIOTomcat(){
        HttpServer server = new BIOTomcat(8081);
        server.start();
    }
}