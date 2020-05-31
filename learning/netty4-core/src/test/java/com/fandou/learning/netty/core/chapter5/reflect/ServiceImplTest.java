package com.fandou.learning.netty.core.chapter5.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 反射API的测试
 */
class ServiceImplTest {
    @Test
    void testServiceImpl()  {
        String className = "com.fandou.netty.netty4core.chapter5.reflect.ServiceImpl";
        String simpleClassName = "ServiceImpl";

        // 1.获取对象类型
        Class<?> clazz = null;
        Object serviceImpl = null;

        // 方式一: 直接通过类的class属性获取
        clazz = ServiceImpl.class;

        // 断言完整类名
        assertEquals(className,clazz.getName());
        assertEquals(className,clazz.getTypeName());
        assertEquals(className,clazz.getCanonicalName());
        // 断言简单类名
        assertEquals(simpleClassName,clazz.getSimpleName());

        // 方式二：通过对象实例的getClass()方法获取
        serviceImpl = new ServiceImpl("通过new关键字调用有参构造函数实例化对象");
        clazz = serviceImpl.getClass();
        // 断言完整类名
        assertEquals(className,clazz.getName());
        assertEquals(className,clazz.getTypeName());
        assertEquals(className,clazz.getCanonicalName());
        // 断言简单类名
        assertEquals(simpleClassName,clazz.getSimpleName());

        // 方式三：通过Class.forName()方法获取，若不存在将抛出ClassNotFoundException异常
        try {
            clazz = Class.forName(className);
            // 断言完整类名
            assertEquals(className,clazz.getName());
            assertEquals(className,clazz.getTypeName());
            assertEquals(className,clazz.getCanonicalName());
            // 断言简单类名
            assertEquals(simpleClassName,clazz.getSimpleName());
        } catch (ClassNotFoundException e) {
            System.err.println("类型不存在...");
            fail();
        }

        // 方式四：若不存在将抛出ClassNotFoundException异常
        try {
            // 通过同一个类加载器获取：根据类的完整名加载类型
            clazz = Service.class.getClassLoader().loadClass(className);
            // 断言完整类名
            assertEquals(className,clazz.getName());
            assertEquals(className,clazz.getTypeName());
            assertEquals(className,clazz.getCanonicalName());
            // 断言简单类名
            assertEquals(simpleClassName,clazz.getSimpleName());
        } catch (ClassNotFoundException e) {
            System.err.println("类型不存在...");
            fail();
        }

        // 2.获取类的修饰符
        int modifier = clazz.getModifiers();
        // 断言为public
        assertTrue(Modifier.isPublic(modifier));
        // 类的修饰符集合：
        int[] classModifiers = {Modifier.PUBLIC , Modifier.PROTECTED , Modifier.PRIVATE , Modifier.ABSTRACT , Modifier.STATIC , Modifier.FINAL , Modifier.STRICT};
        System.out.print("Java共有" + classModifiers.length + "个类修饰符 => {");
        for (int i = 0; i < classModifiers.length; i++) {
            int modifierValue = classModifiers[i];
            String modifierKeyword = Modifier.toString(modifierValue);
            System.out.print(modifierKeyword + ":" + modifierValue);
            if(i < classModifiers.length -1){
                System.out.print(",");
            }
        }
        System.out.println("}");

        // 3.调用无参构造函数实例化对象
        try {
            serviceImpl = clazz.newInstance();
        } catch (InstantiationException e) {
            System.err.println("实例化时出错...");
        } catch (IllegalAccessException e) {
            System.err.println("非法访问,权限不足...");
        }

        // 4.有参构造函数实例化对象
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class);
            serviceImpl = constructor.newInstance("通过反射调用有参构造函数实例化对象.");
        } catch (NoSuchMethodException e) {
            System.err.println("不存在符合给定的参数条件的构造函数，即方法不存在...");
        } catch (IllegalAccessException e) {
            System.err.println("非法访问,权限不足...");
        } catch (InstantiationException e) {
            System.err.println("实例化时出错...");
        } catch (InvocationTargetException e) {
            System.err.println("调用构造方法实例化时出错...");
        }

        // 5.获取所有构造函数
        Constructor<?>[] constructors = clazz.getConstructors();
        System.out.print("该类有" + constructors.length + "个构造函数 => {");
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            System.out.print(constructor.toString());
            if(i < constructors.length -1){
                System.out.print(",");
            }
        }
        System.out.println("}");

        // 6.获取成员变量
        String keyFieldName = "key";
        String limitFieldName = "limit";
        String codeFieldName = "code";

        Field key = null;
        Field limit = null;
        Field code = null;

        // 方式一
        try {
            // getField方法访问私有成员或保护成员变量会抛出NoSuchFieldException异常
            key = clazz.getField(keyFieldName);
            fail();
        } catch (NoSuchFieldException e) {
            System.err.println("没有此成员变量或无权限访问...");
        }

        try {

            // getField方法只能访问public修饰的成员变量
            code = clazz.getField(codeFieldName);

            // 使用getDeclaredField访问私有成员变量
            key = clazz.getDeclaredField(keyFieldName);

            try {
                // getField方法访问私有成员或保护成员变量会抛出NoSuchFieldException异常
                limit = clazz.getField(limitFieldName);
                fail();
            }
            catch (NoSuchFieldException e){
                System.err.println("没有此成员变量或无权限访问，尝试使用getDeclaredField访问获取...");
                limit = clazz.getDeclaredField(limitFieldName);
            }

            // 打印成员变量信息：get方法获取实例对象对应成员变量的值，此方法可以访问public、protected成员变量的值
            System.out.println(code.getName() + " : {" + code.getType() + "," + Modifier.toString(code.getModifiers()) + ","  + code.get(serviceImpl) + "}");
            System.out.println(limit.getName() + " : {" + limit.getType() + "," + Modifier.toString(limit.getModifiers()) + "," + limit.get(serviceImpl) + "}");
            System.out.println(key.getName() + " : {" + key.getType() + "," + Modifier.toString(key.getModifiers()) + "}");
            // 预期将抛出异常
            System.out.println("key ===> " + key.get(serviceImpl));
            fail();
        } catch (NoSuchFieldException e) {
            System.err.println("没有此成员变量或无权限访问...");
        } catch (IllegalAccessException e) {
            System.err.println("非法访问,权限不足...");
        }

        // setAccessible方法可以将private的成员变量设置可访问，之后就可以正常操作成员变量，比如get/set方法取值/赋值
        key.setAccessible(true);
        try {
            System.out.println("设置可访问后取值key ===> " + key.get(serviceImpl));
            key.set(serviceImpl,"设置key的值");
            System.out.println("key新值 ===> " + key.get(serviceImpl));

            code.set(serviceImpl,"设置code的值");
            limit.set(serviceImpl,100);

            key.set(null,"可以为null对象赋值码");
            fail();
        } catch (IllegalAccessException e) {
            System.err.println("非法访问,权限不足...");
        } catch (NullPointerException e) {
            System.err.println("空指针..." + e.getMessage());
        }

        // 获取所有成员变量
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields){
            // 设置为可访问
            field.setAccessible(true);
            try {
                System.out.println(field.getName() + " : {" + field.getType() + "," + Modifier.toString(field.getModifiers()) + ","  + field.get(serviceImpl) + "}");
            } catch (IllegalAccessException e) {
                System.err.println("非法访问,权限不足...");
            }
        }

        // 7.获取普通方法：含含main方法，但不包括构造方法，要获取构造方法请使用getConstructors或getConstructor方法
        String processMethodName = "process";
        Class<Integer> processParameterType = int.class;

        Method process = null;
        Method setLimit = null;
        Method init = null;

        try {
            // getMethod获取public修饰的方法：获取带有一个int类型参数名字为process的公共方法
            process = clazz.getMethod(processMethodName,processParameterType);
            System.out.println(processMethodName + "方法返回值类型=>" + process.getReturnType().getTypeName());

            // 获取无参数名字为setLimit的保护方法：预期将抛出异常
            setLimit = clazz.getMethod("setLimit",int.class);
            fail();
            System.out.println(setLimit.getName() + "方法参数个数=>" + setLimit.getParameterCount());

            // 获取无参数名字为init的私有方法：预期将抛出异常
            init = clazz.getMethod("init");
            System.out.println(init.getName() + "方法参数个数=>" + init.getParameterCount());
        } catch (NoSuchMethodException e) {
            System.err.println("没有此方法或无权限访问...");
        }

        try{
            // 调用serviceImpl对象的process方法，掺入的整数型参数值为10
            process.invoke(serviceImpl,10);


            // 使用getDeclaredMethod方法获取private或protected方法
            setLimit = clazz.getDeclaredMethod("setLimit",int.class);
            System.out.println(setLimit.getName() + "方法参数个数=>" + setLimit.getParameterCount());
            // 调用serviceImpl对象的setLimit方法修改其limit值为200：protected和public均可调用invoke方法
            setLimit.invoke(serviceImpl,200);

            init = clazz.getDeclaredMethod("init");
            System.out.println(init.getName() + "方法参数个数=>" + init.getParameterCount());

            // 调用serviceImpl对象的init方法：私有方法需要调用setAccessible方法设置为可访问
            init.setAccessible(true);
            init.invoke(serviceImpl);

            // 获取所有方法，并调用getter方法获取serverImpl对象对应的成员值
            Method[] methods = clazz.getDeclaredMethods();
            for(Method method : methods){
                String name = method.getName();
                int parameterCount = method.getParameterCount();
                Class<?> returnType = method.getReturnType();
                String returnTypeName = returnType.getTypeName();

                // 获取方法的所有参数信息
                StringBuilder params = new StringBuilder("");
                Parameter[] parameters = method.getParameters();
                for(Parameter parameter : parameters){
                    params.append(Modifier.toString(parameter.getModifiers()) + " " + parameter.getType().getName() + " " + parameter.getName() + " ");
                }

                Object reutrnValue = null;
                if(0 == parameterCount){
                    method.setAccessible(true);
                    reutrnValue = method.invoke(serviceImpl);
                    /*
                    // 如果返回值类型不为空void
                    if(!Void.class.equals(returnType)) {
                        reutrnValue = method.invoke(serviceImpl);
                    }
                    else{
                        method.invoke(serviceImpl);
                    }
                    */
                }

                System.out.println(name + "{parameterCount : " + parameterCount + "[" + params.toString() + "],returnTypeName:" + returnTypeName + "}  => " + reutrnValue);
            }

            // 调用静态方法
            Method main = clazz.getMethod("main",String[].class);
            // invoke方法第二个参数时可变参数，String[]为一个参数。如果是多个参数，args应该声明为数组,也可以声明多个参数传递
            Object args = new String[]{"Java","Python"};
            // 调用静态方法时，obj参数可以为null
            main.invoke(null,args);

            Class<?>[] appendArgs = new Class[]{int.class,String.class};
            Method append = clazz.getDeclaredMethod("append",appendArgs);
            int a = 10;
            String b = "测试";
            Object value = append.invoke(null,a,b);
            System.out.println("value => " + value);
            // 传入非null调用静态方法
            System.out.println("value => " + append.invoke(serviceImpl,a,b));

            // 8.注解


            // 9.其它
            // 判断是否为基本类型
            assertFalse(String.class.isPrimitive());
            assertTrue(int.class.isPrimitive());
            assertFalse(int[].class.isPrimitive());

            // 判断是否为数组
            assertTrue(String[].class.isArray());

            // 基本类型和包装类型
            assertTrue(int.class == Integer.TYPE);
            assertFalse(int.class == Integer.class);
            assertFalse(int.class.equals(Integer.class));
            assertFalse(int[].class.equals(Integer[].class));
            // 下面的==无法通过编译
            // assertFalse(int[].class == Integer[].class);

        }
        catch (NoSuchMethodException ex){
            System.err.println("没有此方法或无权限访问...");
        } catch (IllegalAccessException e) {
            System.err.println("没有访问权限...");
        } catch (InvocationTargetException e) {
            System.err.println("调用方法发生异常...");
        }
    }
}