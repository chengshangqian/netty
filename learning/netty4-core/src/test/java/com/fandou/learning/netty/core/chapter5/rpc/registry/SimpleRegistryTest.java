package com.fandou.learning.netty.core.chapter5.rpc.registry;

import org.junit.jupiter.api.Test;

class SimpleRegistryTest {

    @Test
    void testSimpleRegistry(){
        SimpleRegistry  registry = new SimpleRegistry(8080);
        registry.start();
    }
}