package com.fandou.learning.netty.core.chapter5.classloader;

/**
 * 自定义类加载器
 * JVM中类加载器ClassLoader的defineClass方法时protected，可以通过继承访问，然后在新的类中开放，以实现自定以加载类:
 * 即可以不需要声明引入(import)，而时通过自定义的类加载器直接加载想要使用的类的.class文件即可调用该类
 */
public class MyClassLoader extends ClassLoader {
    /**
     * 自定义类的加载
     *
     * @param name 类名。类在字节码中的名称，不知道可以传入null
     * @param b 类的字节码数组即.class文件内容
     * @param off offset,在数组b中的读取类的内容的开始索引
     * @param len 类的内容的长度
     * @return
     */
    public Class<?> defineMyClass(String name,byte[] b,int off,int len){
        return super.defineClass(name,b,off,len);
    }
}
