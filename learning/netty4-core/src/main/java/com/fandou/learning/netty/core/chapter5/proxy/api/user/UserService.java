package com.fandou.learning.netty.core.chapter5.proxy.api.user;

/**
 * 用户服务接口：模拟
 */
public interface UserService {

    /**
     * 添加一个新用户
     *
     * @param name 用户名
     * @return 创建成功返回用户id，失败返回-1L
     */
    public long create(String name);

    /**
     *
     * @param id 要修改的用户id
     * @param name 新的用户名
     * @return 修改操作状态：成功或失败
     */
    public boolean modify(long id,String name);

    /**
     * 删除一个用户
     * @param id 用户id
     * @return 删除操作状态：成功或失败
     */
    public boolean delete(long id);

    /**
     * 查询用户id
     *
     * @param name 要查询的用户名
     * @return 存在用户则返回用户id，否则返回-1L
     */
    public long query(String name);
}
