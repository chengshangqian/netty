package com.fandou.learning.netty.core.chapter5.proxy.stat;

import com.fandou.learning.netty.core.chapter5.proxy.api.order.JDOrderService;
import com.fandou.learning.netty.core.chapter5.proxy.api.order.OrderService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceProxyTest {
    @Test
    void testOrderService(){
        OrderService orderServiceImpl = new OrderService();
        OrderService jdOrderServiceImpl = new JDOrderService();

        OrderServiceProxy orderService = new OrderServiceProxy(orderServiceImpl);
        OrderServiceProxy jdOrderService = new OrderServiceProxy(jdOrderServiceImpl);

        // 一般订单服务
        long orderId = orderService.create("华为手机");
        orderService.cancel(orderId);
        try{
            // 预期发生异常：java.lang.ClassCastException
            ((JDOrderService)orderServiceImpl).query("华为手机");
            fail();
            ((JDOrderService)orderServiceImpl).modify(orderId,"小米手机");
        }
        catch (Exception e){
            e.printStackTrace();
        }

        // 京东订单服务
        long jdOrderId = jdOrderService.create("苹果笔记本");
        jdOrderService.cancel(jdOrderId);
        // 尝试强转
        ((JDOrderService)jdOrderServiceImpl).query("苹果笔记本");
        ((JDOrderService)jdOrderServiceImpl).modify(jdOrderId,"ThinkPad笔记本");
    }
}