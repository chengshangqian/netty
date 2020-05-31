package com.fandou.learning.netty.core.chapter5.proxy.stat;

import com.fandou.learning.netty.core.chapter5.proxy.api.order.OrderService;

/**
 * 订单服务静态代理
 */
public class OrderServiceProxy {
    private final OrderService orderService;

    public OrderServiceProxy(OrderService orderService) {
        this.orderService = orderService;
    }

    public long create(String order){
        return orderService.create(order);
    }

    public boolean cancel(long id){
        return orderService.cancel(id);
    }
}
