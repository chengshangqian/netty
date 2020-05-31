package com.fandou.learning.netty.core.chapter5.proxy.dynamic.javassist;

import javassist.*;

public class DefaultJavassistProxyObjectBuilder<T> {

    // 目标类的包名
    private String packageName;
    // 目标类的类名
    private String classSimpleName;

    // 需要用到第三方jar或.class的目录：即相当于classpath
    private Class<?>[] classpathClasses;

    // 目标类引用到的其它类的包
    private String[] referenceClassPackageNames;

    // 目标类实现的接口
    private String[] interfaceNames;

    // 目标类的成员
    private String[] fields;

    // 目标类的构造函数
    private String[] constructors;

    // 目标类的方法
    private String[] methods;

    // 与方法对应的方法修饰符
    private int[] methodModifiers;

    public String getPackageName() {
        return packageName;
    }

    public DefaultJavassistProxyObjectBuilder setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getClassSimpleName() {
        return classSimpleName;
    }

    public DefaultJavassistProxyObjectBuilder setClassSimpleName(String classSimpleName) {
        this.classSimpleName = classSimpleName;
        return this;
    }

    public Class<?>[] getClasspathClasses() {
        return classpathClasses;
    }

    public DefaultJavassistProxyObjectBuilder setClasspathClasses(Class<?>[] classpathClasses) {
        this.classpathClasses = classpathClasses;
        return this;
    }

    public String[] getReferenceClassPackageNames() {
        return referenceClassPackageNames;
    }

    public DefaultJavassistProxyObjectBuilder setReferenceClassPackageNames(String[] referenceClassPackageNames) {
        this.referenceClassPackageNames = referenceClassPackageNames;
        return this;
    }

    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    public DefaultJavassistProxyObjectBuilder setInterfaceNames(String[] interfaceNames) {
        this.interfaceNames = interfaceNames;
        return this;
    }

    public String[] getFields() {
        return fields;
    }

    public DefaultJavassistProxyObjectBuilder setFields(String[] fields) {
        this.fields = fields;
        return this;
    }

    public String[] getMethods() {
        return methods;
    }

    public DefaultJavassistProxyObjectBuilder setMethods(String[] methods) {
        this.methods = methods;
        return this;
    }

    public int[] getMethodModifiers() {
        return methodModifiers;
    }

    public DefaultJavassistProxyObjectBuilder setMethodModifiers(int[] methodModifiers) {
        this.methodModifiers = methodModifiers;
        return this;
    }

    /**
     * 构建动态创建的对象类型
     *
     * @return
     * @throws CannotCompileException
     */
    public Class<T> build() throws CannotCompileException, NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        // 添加classpath
        for(Class<?> classpathClass : classpathClasses){
            pool.insertClassPath(new ClassClassPath(classpathClass));
        }

        // 声明目标类所在的包
        pool.importPackage(packageName);

        // 引入需要用到的第三方或其它类的包
        for(String referenceClassPackageName : referenceClassPackageNames){
            pool.importPackage(referenceClassPackageName);
        }

        // 动态创建目标类
        CtClass ctClass = pool.makeClass(String.format("%s.%s",packageName,classSimpleName));

        // 添加目标类要实现的接口
        for(String interfcs : interfaceNames){
            ctClass.addInterface(pool.getCtClass(interfcs));
        }

        // 添加成员变量
        for(String field : fields){
            CtField ctField = CtField.make(field,ctClass);
            ctClass.addField(ctField);
        }

        // 添加构造函数
        for (String constructor : constructors){
            // TODO
            /*
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{pool.getCtClass(constructor)},ctClass);
            ctClass.addConstructor(ctConstructor);
            */
        }

        // 添加方法及其修饰符
        for(int i = 0; i < methods.length; i++){
            String method = methods[i];
            CtMethod ctMethod = CtMethod.make(method,ctClass);
            if(null != methodModifiers && methodModifiers[i] > 0) {
                ctMethod.setModifiers(methodModifiers[i]);
            }
            ctClass.addMethod(ctMethod);
        }

        return (Class<T>) ctClass.toClass();
    }

    public static void main(String[] args) {
        DefaultJavassistProxyObjectBuilder builder = new DefaultJavassistProxyObjectBuilder();
        Class<?>[] classpath = new Class[]{};
        builder.setClasspathClasses(new Class[]{DefaultJavassistProxyObjectBuilder.class});
        String name = DefaultJavassistProxyObjectBuilder.class.getPackage().getName();
        System.out.println(name);
    }
}
