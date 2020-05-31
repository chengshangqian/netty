package com.fandou.learning.netty.core.chapter5.proxy.dynamic.cglib;

import net.sf.cglib.proxy.Enhancer;

/**
 *
 */
public class DefaultCglibProxyFactory {
    public static <T> T createProxy(Class<T> targetClass){
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(new DefaultMethodInterceptor());
        return (T) enhancer.create();
    }
}
