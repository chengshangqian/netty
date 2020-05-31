package com.fandou.learning.netty.core.chapter5.proxy.stat;

import com.fandou.learning.netty.core.chapter5.proxy.api.user.UserService;

/**
 * 用户服务静态代理类：静态代理，代理类UserServiceProxy本身implements目标接口UserService，然后内部代理一个真正的具体实现类
 */
public class UserServiceProxy implements UserService {
    /**
     * 代理的真正的具体实现类
     */
    private final UserService userService;

    /**
     * 初始化代理的实现类：外部使用代理类时，需要先指派真实的具体实现类
     *
     * @param userService 用户服务的具体实现类
     */
    public UserServiceProxy(UserService userService) {
        this.userService = userService;
    }

    @Override
    public long create(String name) {
        // 调用代理的真正的实现类对应的方法
        return userService.create(name);
    }

    @Override
    public boolean modify(long id, String name) {
        // 调用代理的真正的实现类对应的方法
        return userService.modify(id,name);
    }

    @Override
    public boolean delete(long id) {
        // 调用代理的真正的实现类对应的方法
        return userService.delete(id);
    }

    @Override
    public long query(String name) {
        // 调用代理的真正的实现类对应的方法
        return userService.query(name);
    }
}
