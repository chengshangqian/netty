package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import com.fandou.learning.netty.core.chapter5.proxy.api.user.DefaultUserServiceImpl;
import com.fandou.learning.netty.core.chapter5.proxy.api.user.TeacherServiceImpl;
import com.fandou.learning.netty.core.chapter5.proxy.api.user.UserService;
import com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.chain.BossInterceptor;
import com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.chain.DepartmentLeaderInterceptor;
import com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.chain.GroupLeaderInterceptor;
import org.junit.jupiter.api.Test;

class DefaultInterceptorProxyFactoryTest {
    @Test
    void testDefaultInterceptorProxyFactory() throws IllegalAccessException, InstantiationException {
        UserService userService = DefaultInterceptorProxyFactory.createProxy(DefaultUserServiceImpl.class);
        userService.create("张三");

        userService = DefaultInterceptorProxyFactory.createProxy(TeacherServiceImpl.class, MyInterceptor.class);
        userService.create("李四");

        // 责任链模式
        UserService groupLeader = DefaultInterceptorProxyFactory.createProxy(TeacherServiceImpl.class, GroupLeaderInterceptor.class);
        UserService departmentLeader = DefaultInterceptorProxyFactory.createProxy(groupLeader, DepartmentLeaderInterceptor.class);
        UserService boss = DefaultInterceptorProxyFactory.createProxy(departmentLeader, BossInterceptor.class);
        boss.create("小明请假条");
    }
}