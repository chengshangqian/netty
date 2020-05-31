package com.fandou.learning.netty.action.chapter13;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.InetSocketAddress;

class LogEventBroadcasterTest {
    @Test
    void testLogEventBroadcaster() throws Exception {
        String file = "D:/tmp/logs/springbootvue/test.log";
        String hostname = "255.255.255.255";
        int port = 9999;
        LogEventBroadcaster broadcaster = new LogEventBroadcaster(new InetSocketAddress(hostname,port),new File(file));
        try{
            broadcaster.run();
        }
        finally {
            broadcaster.stop();
        }
    }
}