package com.fandou.learning.netty.core.chapter5.proxy.api.user;

/**
 * 用户服务实现：缺省的用户服务实现类
 */
public class DefaultUserServiceImpl implements UserService {

    /**
     * 对象id：测试用
     */
    private final long objectId;

    public DefaultUserServiceImpl() {
        objectId = System.nanoTime();
    }

    @Override
    public long create(String name) {
        System.out.println(objectId + " => 创建了一个用户[" + name + "].");
        return System.nanoTime();
    }

    @Override
    public boolean modify(long id, String name) {
        System.out.println(objectId + " => 用户[" + id + "]的用户名已修改为[" + name + "].");
        return true;
    }

    @Override
    public boolean delete(long id) {
        System.out.println(objectId + " => 用户[" + id + "]已删除.");
        return true;
    }

    @Override
    public long query(String name) {
        System.out.println(objectId + " => 查询到用户[" + name + "].");
        return System.nanoTime();
    }
}
