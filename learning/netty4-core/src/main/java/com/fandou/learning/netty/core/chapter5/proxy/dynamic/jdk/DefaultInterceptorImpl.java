package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DefaultInterceptorImpl implements Interceptor {
    @Override
    public Object around(Object proxy, Object target, Method method, Object[] args) {
        try {
            System.out.println("around begin...");
            Object result = method.invoke(target,args);
            System.out.println("around end...");
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
        System.out.println("before...");
        return true;
    }

    @Override
    public void after(Object proxy, Object target, Method method, Object[] args) {
        System.out.println("after...");
    }
}
