package com.fandou.learning.netty.core.chapter5.rpc.provider;

import com.fandou.learning.netty.core.chapter5.rpc.api.HelloService;

public class SimpleHelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "你好," + name + "!";
    }
}
