package com.fandou.learning.netty.core.chapter5.proxy.dynamic.cglib;

import com.fandou.learning.netty.core.chapter5.proxy.api.order.JDOrderService;
import com.fandou.learning.netty.core.chapter5.proxy.api.order.OrderService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCglibProxyFactoryTest {
    @Test
    void testDefaultCglibProxyFactory(){
        OrderService orderService = DefaultCglibProxyFactory.createProxy(OrderService.class);
        long orderId = orderService.create("苹果IPHONE6+手机");
        assertTrue(orderService.cancel(orderId));

        JDOrderService jdOrderService = DefaultCglibProxyFactory.createProxy(JDOrderService.class);
        long jdOrderId = jdOrderService.create("华为笔记本");
        System.out.println("jdOrderId => " + jdOrderId);
        assertTrue(jdOrderService.modify(jdOrderId,"MacBookPro"));
    }
}