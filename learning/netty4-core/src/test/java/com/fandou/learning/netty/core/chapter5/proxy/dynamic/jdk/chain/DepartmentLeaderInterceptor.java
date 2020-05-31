package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.chain;

import com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.Interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 部门经理审批
 */
public class DepartmentLeaderInterceptor implements Interceptor {

    @Override
    public Object around(Object proxy, Object target, Method method, Object[] args) {
        try {
            System.out.println("部门经理同意员工请假...");
            Object result = method.invoke(target,args);
            System.out.println("部门经理将请假结果反馈给工作组组长...");

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
        System.out.println("部门经理同意，提交给公司老板审批...");
        return true;
    }

    @Override
    public void after(Object proxy, Object target, Method method, Object[] args) {
        System.out.println("部门经理将请假结果反馈给工作组组长...");
    }
}
