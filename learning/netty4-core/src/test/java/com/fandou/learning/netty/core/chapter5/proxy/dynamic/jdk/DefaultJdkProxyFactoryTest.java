package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import com.fandou.learning.netty.core.chapter5.proxy.api.user.DefaultUserServiceImpl;
import com.fandou.learning.netty.core.chapter5.proxy.api.user.TeacherServiceImpl;
import com.fandou.learning.netty.core.chapter5.proxy.api.user.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultJdkProxyFactoryTest {
    @Test
    void testJdkProxy() throws IllegalAccessException, InstantiationException {
        // 代理一般用户服务DefaultUserServiceImpl实例
        UserService userService = DefaultJdkProxyFactory.createProxy(DefaultUserServiceImpl.class);
        long userId = userService.create("张三");
        assertTrue(userService.delete(userId));

        // 代理教师用户服务类TeacherServiceImpl实例
        userService = DefaultJdkProxyFactory.createProxy(new TeacherServiceImpl());
        long teacherId = userService.create("李四");
        assertTrue(userService.delete(teacherId));
    }
}