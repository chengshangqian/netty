package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MyInterceptor implements Interceptor {

    @Override
    public Object around(Object proxy, Object target, Method method, Object[] args) {
        try {
            System.out.println("开始环绕通知...");
            Object result = method.invoke(target,args);
            System.out.println("结束环绕通知...");
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
        // 不设置前置通知
        return false;
    }

    @Override
    public void after(Object proxy, Object target, Method method, Object[] args) {
        System.out.println("后置通知");
    }
}
