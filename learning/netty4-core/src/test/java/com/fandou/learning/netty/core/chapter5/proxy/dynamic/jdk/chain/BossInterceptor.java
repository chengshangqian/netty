package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.chain;

import com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk.Interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 公司老板审批
 */
public class BossInterceptor implements Interceptor {

    @Override
    public Object around(Object proxy, Object target, Method method, Object[] args) {
        try {
            System.out.println("公司老板同意员工请假...");
            Object result = method.invoke(target,args);
            System.out.println("公司老板将请假结果反馈给部门经理...");

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
        System.out.println("公司老板同意...");
        return true;
    }

    @Override
    public void after(Object proxy, Object target, Method method, Object[] args) {
        System.out.println("公司老板将请假结果反馈给部门经理...");
    }
}
