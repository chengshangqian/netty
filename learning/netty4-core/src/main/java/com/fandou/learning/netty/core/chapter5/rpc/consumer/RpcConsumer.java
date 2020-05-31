package com.fandou.learning.netty.core.chapter5.rpc.consumer;

import com.fandou.learning.netty.core.chapter5.rpc.api.CalcService;
import com.fandou.learning.netty.core.chapter5.rpc.api.HelloService;

public class RpcConsumer {
    private HelloService helloService;
    private CalcService calcService;

    public RpcConsumer(){
        System.out.println("开始初始化RpcConsumer => ");
        helloService = RpcProxy.create(HelloService.class);
        calcService = RpcProxy.create(CalcService.class);
        System.out.println("初始化RpcConsumer完成 => ");
    }

    public void sayHello(){
        System.out.println("开始调用helloService");
        String result = helloService.hello("Netty");
        System.out.println("result => " + result);
    }

    public int calc(String expression){
        System.out.println("开始调用calcService");
        // TODO 解析表达式expression
        // (12/4) + (8/2 * (20 - 5)) + (2 + 2) = 67
        int result = calcService.div(12,4) + calcService.mult(calcService.div(8,2),calcService.sub(20,5)) + calcService.add(2,2);
        System.out.println("result => " + result);
        return result;
    }
}
