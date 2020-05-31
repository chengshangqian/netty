package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import java.lang.reflect.Proxy;

/**
 * 泛型代理类工厂：实现通用的JDK动态代理对象的创建操作
 */
public class DefaultJdkProxyFactory {

    /**
     *  使用JDK动态代理创建代理对象
     *
     * @param targetClass 服务接口的具体实现类类型
     * @return 代理对象实例（代理对象本身，非被代理的目标对象）
     */
    public static <T> T createProxy(Class<? extends T> targetClass) throws InstantiationException, IllegalAccessException {

        // 目标对象的类加载器
        ClassLoader cl = targetClass.getClassLoader();

        // 目标对象实现的接口
        Class<?>[] interfaces = targetClass.getInterfaces();

        // 代理对象调用目标对象的方法调用的处理器
        DefaultInvocationHandler invocationHandler = new DefaultInvocationHandler(targetClass);

        // 创建代理对象实例
        T proxy = (T) Proxy.newProxyInstance(cl,interfaces,invocationHandler);

        // 返回代理对象
        return proxy;
    }

    /**
     *  使用JDK动态代理创建代理对象
     *
     * @param target 服务接口的具体实现类实例，即目标对象
     * @return 代理对象实例（代理对象本身，非被代理的目标对象）
     */
    public static <T> T createProxy(T target) throws InstantiationException, IllegalAccessException {

        // 目标对象的类加载器
        ClassLoader cl = target.getClass().getClassLoader();

        // 目标对象实现的接口
        Class<?>[] interfaces = target.getClass().getInterfaces();

        // 代理对象调用目标对象的方法调用的处理器
        DefaultInvocationHandler invocationHandler = new DefaultInvocationHandler(target);

        // 创建代理对象实例
        T proxy = (T) Proxy.newProxyInstance(cl,interfaces,invocationHandler);

        // 返回代理对象
        return proxy;
    }
}
