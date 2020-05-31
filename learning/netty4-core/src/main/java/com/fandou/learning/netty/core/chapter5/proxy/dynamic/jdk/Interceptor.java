package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import java.lang.reflect.Method;

/**
 * 拦截器接口
 */
public interface Interceptor {
    Object around(Object proxy, Object target, Method method, Object[] args);
    boolean before(Object proxy, Object target, Method method, Object[] args);
    void after(Object proxy, Object target, Method method, Object[] args);
}
