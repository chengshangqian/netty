package com.fandou.learning.netty.core.chapter5.proxy.dynamic.cglib;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * CGLIB方法过滤器：对方法进行增强/切入
 */
public class DefaultMethodInterceptor implements MethodInterceptor {

    /**
     *  过滤方法的调用
     *
     * @param obj 代理对象
     * @param method 目标对象方法
     * @param args 方法调用参数
     * @param proxy 目标对象代理方法
     * @return 调用目标对象方法结果返回值，如果有的话
     * @throws Throwable
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        // 调用方法前做一些事情
        System.out.println("method => " + method.getName());

        // 调用目标对象的方法：正确的调用
        Object result = proxy.invokeSuper(obj,args);

        // 下面的两种调用API将会产生内存溢出错误：StackOverflowError
        // Object result = proxy.invoke(obj,args);
        // Object result = method.invoke(obj,args);

        // 调用方法后做一些事情
        System.out.println("result => " + result);

        // 返回值之前做一些事情
        return result;
    }
}
