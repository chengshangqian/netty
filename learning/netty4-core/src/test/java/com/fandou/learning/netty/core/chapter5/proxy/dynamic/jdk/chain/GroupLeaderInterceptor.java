package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.chain;

import com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.Interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 工作组组长审批
 */
public class GroupLeaderInterceptor implements Interceptor {

    @Override
    public Object around(Object proxy, Object target, Method method, Object[] args) {
        try {
            System.out.println("组长同意员工请假...");
            Object result = method.invoke(target,args);
            System.out.println("组长将请假结果反馈给员工...");

            return result;

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean before(Object proxy, Object target, Method method, Object[] args) {
        System.out.println("组长同意，提交给部门经理审批...");
        return true;
    }

    @Override
    public void after(Object proxy, Object target, Method method, Object[] args) {
        System.out.println("组长将请假结果反馈给员工...");
    }
}
