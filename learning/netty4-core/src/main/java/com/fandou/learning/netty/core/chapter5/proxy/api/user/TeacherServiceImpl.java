package com.fandou.learning.netty.core.chapter5.proxy.api.user;

/**
 * 教师用户服务实现类型
 */
public class TeacherServiceImpl implements UserService {

    /**
     * 对象id：测试用
     */
    private final long objectId;

    public TeacherServiceImpl() {
        objectId = System.nanoTime();
    }

    @Override
    public long create(String name) {
        System.out.println(objectId + " => 创建了一个教师用户["  + name + "].");
        return System.nanoTime();
    }

    @Override
    public boolean modify(long id, String name) {
        System.out.println(objectId + " => 教师用户[" + id + "]的用户名已修改为[" + name + "].");
        return true;
    }

    @Override
    public boolean delete(long id) {
        System.out.println(objectId + " => 教师用户[" + id + "]已删除.");
        return true;
    }

    @Override
    public long query(String name) {
        System.out.println(objectId + " => 查询到教师用户[" + name + "].");
        return System.nanoTime();
    }
}
