package com.fandou.learning.netty.core.chapter5.rpc.registry;

import com.fandou.learning.netty.core.chapter5.rpc.protocol.InvokerProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryHandler extends ChannelInboundHandlerAdapter {

    public static Map<String,Object> registryMapping = new ConcurrentHashMap<String,Object>();

    private List<String> classNames = new ArrayList<String>();

    public RegistryHandler(){
        scanProviders("com.fandou.netty.netty4core.chapter5.netty.rpc.provider");
        doRegister();
    }

    /**
     * 每次调用时扫描
     * @param packages
     */
    private void scanProviders(String packages) {
        System.out.println("扫描服务提供者...");
        URL url = this.getClass().getClassLoader().getResource(packages.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for(File file : dir.listFiles()){
            if(file.isDirectory()){
                // 递归扫描
                scanProviders(packages + "." + file.getName());
            }
            else{
                classNames.add(packages + "." + file.getName().replaceAll(".class","").trim());
            }
        }
    }


    private void doRegister() {
        if(classNames.isEmpty()){
            return;
        }
        System.out.println("注册服务提供者...");
        for(String className : classNames){
            try{
                Class<?> clazz = Class.forName(className);
                Class<?> i = clazz.getInterfaces()[0];
                registryMapping.put(i.getName(),clazz.newInstance());
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object result  = new Object();
        InvokerProtocol request = (InvokerProtocol)msg;
        System.out.println("接收到远程调用协议请求request => " + request);
        if(registryMapping.containsKey(request.getClassName())){
            Object clazz = registryMapping.get(request.getClassName());
            Method method = clazz.getClass().getMethod(request.getMethodName(),request.getParams());
            result = method.invoke(clazz,request.getValues());
            System.out.println("调用服务提供者获取结果result => " + result);
        }

        // 将结果发送回消费者
        System.out.println("发送远程调用结果回消费者端... ");
        ctx.writeAndFlush(result);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
