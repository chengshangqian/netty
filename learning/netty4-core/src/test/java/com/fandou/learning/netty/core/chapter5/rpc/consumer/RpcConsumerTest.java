package com.fandou.learning.netty.core.chapter5.rpc.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RpcConsumerTest {

    @Test
    void sayHello() {
        RpcConsumer consumer = new RpcConsumer();
        consumer.sayHello();
    }

    @Test
    void calc() {
        RpcConsumer consumer = new RpcConsumer();
        int result = consumer.calc("(12/4) + (8/2 * (20 - 5)) + (2 + 2)");
        assertEquals(67,result);
    }
}