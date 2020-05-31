package com.fandou.learning.netty.core.chapter5.proxy.stat;

import com.fandou.learning.netty.core.chapter5.proxy.api.user.DefaultUserServiceImpl;
import com.fandou.learning.netty.core.chapter5.proxy.api.user.TeacherServiceImpl;
import com.fandou.learning.netty.core.chapter5.proxy.api.user.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 静态代理测试
 */
class UserServiceProxyTest {
    @Test
    void testUserServiceProxy(){
        // 缺省用户服务和教师服务实现
        UserService userServiceImpl = new DefaultUserServiceImpl();
        UserService teacherServiceImpl = new TeacherServiceImpl();

        // 创建缺省用户服务和教师服务对应的代理
        UserServiceProxy userService = new UserServiceProxy(userServiceImpl);
        UserServiceProxy teacherService = new UserServiceProxy(teacherServiceImpl);

        // 创建用户
        long userId = userService.create("张三");
        assertNotEquals(-1L,userId);
        // 修改用户名
        assertTrue(userService.modify(userId,"张三丰"));
        // 查询用户id
        assertTrue(userService.query("张三丰") >= userId);
        // 删除用户
        assertTrue(userService.delete(userId));

        // 创建教师
        long teacherId = teacherService.create("李四");
        assertNotEquals(-1L,teacherId);
        // 修改教师名
        assertTrue(teacherService.modify(teacherId,"李四川"));
        // 查询教师id
        assertTrue(teacherService.query("李四川") >= teacherId);
        // 删除教师
        assertTrue(teacherService.delete(teacherId));
    }
}