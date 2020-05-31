package com.fandou.learning.netty.core.chapter5.proxy.dynamic.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK代理类调用目标对象方法时的调用处理器：可以增强/切入目标对象的方法
 *
 * @param <T> 代理的目标对象类
 */
public class DefaultInterceptorInvocationHandler<T> implements InvocationHandler {

    /**
     * 被代理的目标对象类型
     */
    private final Class<T> targetClass;

    /**
     * 被代理的目标对象实例
     */
    private T target;

    /**
     * 过滤器实现类类型
     */
    private Class<? extends Interceptor> interceptorClass;

    public DefaultInterceptorInvocationHandler(Class<T> targetClass, Class<? extends Interceptor>  interceptorClass) throws IllegalAccessException, InstantiationException {
        this(targetClass,targetClass.newInstance(),interceptorClass);
    }

    /**
     * 指定被代理的目标对象类型和实例
     *
     * @param target
     */
    public DefaultInterceptorInvocationHandler(T target, Class<? extends Interceptor>  interceptorClass) {
        this((Class<T>)target.getClass(),target,interceptorClass);
    }

    /**
     * 指定被代理的目标对象类型和实例
     *
     * @param targetClass
     * @param target
     */
    public DefaultInterceptorInvocationHandler(Class<T> targetClass, T target, Class<? extends Interceptor>  interceptorClass) {
        this.targetClass = targetClass;
        this.target = target;
        this.interceptorClass = interceptorClass;
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
        // 1.没有配置拦截器
        if(null == interceptorClass){
            return method.invoke(target,args);
        }

        // 2.有配置拦截器
        Object result = null;
        Interceptor interceptor = interceptorClass.newInstance();
        // 调用方法前做一些事情
        System.out.println("method => " + method.getName());
        if(interceptor.before(proxy,target,method,args)){
            result = method.invoke(target,args);
        }
        else {
            // 环绕通知
            result = interceptor.around(proxy,target,method,args);
        }

        // 调用方法后做一些事情
        interceptor.after(proxy,target,method,args);
        System.out.println("result => " + result);

        return result;
    }
}
