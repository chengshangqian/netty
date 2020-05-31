package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK代理类调用目标对象方法时的调用处理器：可以增强/切入目标对象的方法
 *
 * @param <T> 代理的目标对象类
 */
public class DefaultInvocationHandler<T> implements InvocationHandler {

    /**
     * 被代理的目标对象类型
     */
    private final Class<T> targetClass;

    /**
     * 被代理的目标对象实例
     */
    private T target;

    public DefaultInvocationHandler(Class<T> targetClass) throws IllegalAccessException, InstantiationException {
        this(targetClass,targetClass.newInstance());
    }

    /**
     * 指定被代理的目标对象类型和实例
     *
     * @param target
     */
    public DefaultInvocationHandler(T target) {
        this((Class<T>)target.getClass(),target);
    }

    /**
     * 指定被代理的目标对象类型和实例
     *
     * @param targetClass
     * @param target
     */
    public DefaultInvocationHandler(Class<T> targetClass,T target) {
        this.targetClass = targetClass;
        this.target = target;
    }

    /**
     * 调用目标对象方法：使用代理对象调用被代理的目标对象的方法时触发invoke方法
     *
     * @param proxy 代理对象
     * @param method 被调用的目标对象方法
     * @param args 被调用的目标对象方法所需的参数（值）
     * @return 调用目标对象方法的放回值
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 调用方法前做一些事情
        System.out.println("method => " + method.getName());

        // 调用目标方法
        Object result = method.invoke(target,args);

        // 调用方法后做一些事情
        System.out.println("result => " + result);

        // 返回值之前做一些事情
        return result;
    }
}
