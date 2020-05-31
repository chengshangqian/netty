package com.fandou.learning.netty.core.chapter5.rpc.consumer;

import com.fandou.learning.netty.core.chapter5.rpc.protocol.InvokerProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcProxy {
    public static <T> T create(Class<?> clazz){
        MethodProxy proxy = new MethodProxy(clazz);
        Class<?>[] interfaces = clazz.isInterface() ? new Class[]{clazz} : clazz.getInterfaces();
        T result = (T) Proxy.newProxyInstance(clazz.getClassLoader(),interfaces,proxy);
        return result;
    }

    private static class MethodProxy implements InvocationHandler {
        // 注册中心主机和端口号 TODO
        private String host = "localhost";
        private int port = 8080;

        private Class<?> clazz;
        public MethodProxy(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(Object.class.equals(method.getDeclaringClass())){
                try{
                    return method.invoke(this,args);
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            else {
                return rpcInvoke(proxy,method,args);
            }

            return null;
        }

        public Object rpcInvoke(Object proxy, Method method, Object[] args) {
            System.out.println("开始调用RpcProxy.MethodProxy的rpcInvoke方法 => ");
            InvokerProtocol msg = new InvokerProtocol();
            msg.setClassName(this.clazz.getName());
            msg.setMethodName(method.getName());
            msg.setParams(method.getParameterTypes());
            msg.setValues(args);

            final RpcProxyHandler proxyHandler = new RpcProxyHandler();
            EventLoopGroup group = new NioEventLoopGroup();
            try{
                Bootstrap client = new Bootstrap();
                client.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY,true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast("frameDecoder",new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                                pipeline.addLast("frameEncoder",new LengthFieldPrepender(4));
                                pipeline.addLast("encoder",new ObjectEncoder());
                                pipeline.addLast("decoder",new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));

                                pipeline.addLast("proxyHandler",proxyHandler);
                            }
                        });

                // 连接注册中心主机服务器
                ChannelFuture future = client.connect(host,port).sync();
                System.out.println("远程调用协议 => " + msg);
                future.channel().writeAndFlush(msg).sync();
                System.out.println("远程调用writeAndFlush => ");
                future.channel().closeFuture().sync();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            finally {
                group.shutdownGracefully();
            }

            Object result = proxyHandler.getResponse();
            System.out.println("远程调用回结果 => " + result);
            return result;
        }
    }
}
