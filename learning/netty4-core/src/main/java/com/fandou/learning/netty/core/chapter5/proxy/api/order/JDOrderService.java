package com.fandou.learning.netty.core.chapter5.proxy.api.order;

/**
 * 京东订单服务：继承了基本的订单服务之外，还额外扩展了修改和查询订单的服务
 */
public class JDOrderService extends OrderService {

    @Override
    public long create(String order) {
        System.out.println(objectId + " => 提交了一个京东订单[" + order + "].");
        return System.nanoTime();
    }

    public boolean modify(long id,String order){
        System.out.println(objectId + " => 京东订单[" + id + "]已修改为[" + order + "].");
        return true;
    }

    public long query(String order) {
        System.out.println(objectId + " => 查询到京东订单[" + order + "].");
        return System.nanoTime();
    }
}
