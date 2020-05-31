package com.fandou.learning.netty.action.chapter13;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

class LogEventMonitorTest {
    @Test
    void testLogEventMonitor() throws Exception {
        int port = 9999;
        LogEventMonitor monitor = new LogEventMonitor(new InetSocketAddress(port));
        try{
            Channel channel = monitor.bind();
            System.out.println("正在监听端口" + port);
            channel.closeFuture().sync();
        }
        finally {
            monitor.stop();
        }
    }
}