package com.fandou.learning.netty.core.chapter5.proxy.api.order;

/**
 * 订单服务：模拟
 */
public class OrderService {
    /**
     * 对象id：测试用
     */
    protected final long objectId;

    public OrderService() {
        objectId = System.nanoTime();
    }

    /**
     * 提交一个新订单
     *
     * @param order 订单信息
     */
    public long create(String order){
        System.out.println(objectId + " => 提交了一个新订单：" + order);
        return System.nanoTime();
    }

    /**
     * 取消订单
     *
     * @param id 订单id
     */
    public boolean cancel(long id){
        System.out.println(objectId + " => 订单 " + id + " 已取消.");
        return true;
    }
}
