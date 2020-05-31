package com.fandou.learning.netty.core.chapter5.classloader;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class MyClassLoaderTest {
    @Test
    void testMyClassLoader() throws Exception {
        // 创建自定义的类加载器
        MyClassLoader myClassLoader = new MyClassLoader();

        // 字节数组，保存加载的类文件内容
        byte[] b = new byte[1024];

        // 类文件输入流
        InputStream myUtilsClassFileInputStream = null;

        // 类文件长度
        int len = 0;
        try {
            // 读取MyUtils.class文件字节码
            String myUtilsClassFileLocation = "D:/learning/netty/netty4-core/target/classes/com/fandou/netty/netty4core/chapter5/classloader/MyUtils.class";
            myUtilsClassFileInputStream = new FileInputStream(myUtilsClassFileLocation);
            len = myUtilsClassFileInputStream.read(b);
        }
        finally{
            myUtilsClassFileInputStream.close();
        }

        // 加载本地的一个类文件(的字节码数组)到JVM中，从而创建对应的类型并返回
        Class<?> defineClazz = myClassLoader.defineMyClass(null,b,0,len);
        System.out.println("defineClazz => " + defineClazz.getName());

        // 也可以在其它地方使用下面的方式获取
        String className = "com.fandou.netty.netty4core.chapter5.classloader.MyUtils";
        Class<?> clazz = myClassLoader.loadClass(className);
        System.out.println("clazz => " + clazz.getName());

        // 都是同一个类型对象
        assertEquals(defineClazz,clazz);

        // 测试装载是否成功
        System.out.println("CanonicalName => " + clazz.getCanonicalName());

        // 实例化
        Object myUtils = clazz.newInstance();

        // 使用反射调用实例化对象的方法(签名)：当然也可以通过反射获取更多关于类型的信息
        Method method = clazz.getMethod("println",String.class);

        // 最后，通过自定义的类加载器，在本测试类中，不需要声明import对应的MyUtils类而实现使用MyUtils类创建对象并调用其方法
        method.invoke(myUtils,"你好！");
    }
}