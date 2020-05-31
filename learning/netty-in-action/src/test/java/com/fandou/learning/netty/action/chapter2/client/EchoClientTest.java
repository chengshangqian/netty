package com.fandou.learning.netty.action.chapter2.client;

import org.junit.jupiter.api.Test;

public class EchoClientTest {

    @Test
    void start() throws InterruptedException {
        int count = 10000;
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            new EchoClient("localhost",8088).start();
        }
        long end = System.nanoTime();

        long spentTime = (end - start)/1000000000;
        System.out.println("执行" + count + "次请求总共花费时间：" + spentTime + "秒.");
        System.out.println("服务端处理请求能力：" + (count / spentTime)+"次/秒.");
    }
}
