/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ServerBootstrap，服务器引导
 * 引导Bootstrap的子类，它允许服务器通道ServerChannel的简单引导，即通过ServerBootstrap可以很容易创建一个ServerChannel。
 *
 * allow 允许;准许;给予;允许进入(或出去、通过)
 * bootstrap 独自创立;靠一己之力做成;附属于;与…相联系
 *
 * {@link Bootstrap} sub-class which allows easy bootstrap of {@link ServerChannel}
 *
 */
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {

    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);

    // The order in which child ChannelOptions are applied is important they may depend on each other for validation
    // purposes.
    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> childAttrs = new ConcurrentHashMap<AttributeKey<?>, Object>();
    private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);
    private volatile EventLoopGroup childGroup;
    private volatile ChannelHandler childHandler;

    public ServerBootstrap() {
        logger.info("创建服务器引导实例server...");
    }

    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        childGroup = bootstrap.childGroup;
        childHandler = bootstrap.childHandler;
        synchronized (bootstrap.childOptions) {
            childOptions.putAll(bootstrap.childOptions);
        }
        childAttrs.putAll(bootstrap.childAttrs);
    }

    /**
     * 指定父（接收器）和子（客户端）的实践循环组{@link EventLoopGroup}
     *
     * Specify the {@link EventLoopGroup} which is used for the parent (acceptor) and the child (client).
     */
    @Override
    public ServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }

    /**
     * 分别指定父（接受器）和子（客户端）的实践循环组{@link EventLoopGroup}。
     * 两个事件循环组用来处理{@link ServerChannel}服务器通道和{@link Channel}通道上所有的事件和IO.
     *
     * Set the {@link EventLoopGroup} for the parent (acceptor) and the child (client). These
     * {@link EventLoopGroup}'s are used to handle all the events and IO for {@link ServerChannel} and
     * {@link Channel}'s.
     */
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = ObjectUtil.checkNotNull(childGroup, "childGroup");
        logger.info("设置server的接受器事件循环组acceptorGroup和客户端事件循环组clientGroup参数...");
        return this;
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they get created
     * (after the acceptor accepted the {@link Channel}). Use a value of {@code null} to remove a previous set
     * {@link ChannelOption}.
     */
    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        ObjectUtil.checkNotNull(childOption, "childOption");
        synchronized (childOptions) {
            if (value == null) {
                childOptions.remove(childOption);
            } else {
                childOptions.put(childOption, value);
            }
        }
        return this;
    }

    /**
     * Set the specific {@link AttributeKey} with the given value on every child {@link Channel}. If the value is
     * {@code null} the {@link AttributeKey} is removed
     */
    public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
        ObjectUtil.checkNotNull(childKey, "childKey");
        if (value == null) {
            childAttrs.remove(childKey);
        } else {
            childAttrs.put(childKey, value);
        }
        return this;
    }

    /**
     * Set the {@link ChannelHandler} which is used to serve the request for the {@link Channel}'s.
     */
    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        this.childHandler = ObjectUtil.checkNotNull(childHandler, "childHandler");
        logger.info("设置server的客户端通道处理器childHandler参数{}...",childHandler.getClass().getSimpleName());
        return this;
    }

    /**
     * 初始化服务端套接字通道
     *
     * @param channel 服务端套接字通道实例，比如NioServerSocketChannel实例
     */
    @Override
    void init(Channel channel) {

        // 设置通道选项和属性
        setChannelOptions(channel, newOptionsArray(), logger);
        setAttributes(channel, attrs0().entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY));

        // 获取通道管道pipeline
        ChannelPipeline p = channel.pipeline();

        // 子事件循环组
        final EventLoopGroup currentChildGroup = childGroup;
        // 子通道处理器
        final ChannelHandler currentChildHandler = childHandler;
        // 子通道选项
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(EMPTY_OPTION_ARRAY);
        }
        // 子通道属性
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs = childAttrs.entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY);

        // 为服务端套接字通道管道pipeline添加通道初始化器，初始化器也是一个处理器
        // 注意，目前为止，服务端套接字通道channel还未注册到事件循环(关联的选择器Selector)中，
        // 这是首次将处理器添加到pipeline中，此时由于channel还未注册，将会创建一个PendingHandlerAddedTask任务，在接下来的注册通道过程中，该任务会被执行，
        // 届时，下面代码中的pipeline.addLast(handler);中的handler才会添加到pipeline中
        logger.info("开始为通道添加首个通道初始化器...");
        p.addLast(new ChannelInitializer<Channel>() {
            /**
             * 服务端套接字通道
             * @param ch 服务端套接字通道实例
             */
            @Override
            public void initChannel(final Channel ch) {
                // 获取通道管道pipeline
                final ChannelPipeline pipeline = ch.pipeline();

                // 将handler添加到pipeline中，一般不需要额外设置服务端套接字通道处理器，所以handler一般为null
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                // 新开启一个线程添加一个Acceptor处理器，
                // 其用于将新连接的客户端通道，封装转发给工作线程，以让工作线程处理后续I/O交互
                logger.info("创建并提交为服务端套接字通道添加serverBootstrapAcceptor处理器的任务...");
                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        // 创建按一个接收器适配器实例
                        ServerBootstrapAcceptor serverBootstrapAcceptor = new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs);

                        // 将接收器适配器实例天机道通道管道上，以监听处理通道上的请求
                        // ServerBootstrapAcceptor处理器重载了channelRead方法，当主线程将ServerBootstrapAcceptor处理器添加到pipeline后，
                        // 主线程的通道发生可读事件时，就会触发channelRead方法的执行，此时即可通过ServerBootstrapAcceptor处理器初始化时绑定的相关工作线程通道的
                        // 各种参数，调用对应的事件循环/事件执行器开启线程来执行响应可读事件任务
                        logger.info("开始为服务端套接字通道添加serverBootstrapAcceptor处理器...");
                        pipeline.addLast(serverBootstrapAcceptor);
                        logger.info("结束为服务端套接字通道添加serverBootstrapAcceptor处理器...");
                    }
                });
                logger.info("完成创建并提交为服务端套接字通道添加serverBootstrapAcceptor处理器的任务...");
            }
        });
        logger.info("结束为通道添加首个通道初始化器...");
    }

    @Override
    public ServerBootstrap validate() {
        super.validate();
        if (childHandler == null) {
            throw new IllegalStateException("childHandler not set");
        }
        if (childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            childGroup = config.group();
        }
        return this;
    }

    /**
     * 服务器引导适配器，它是通道入站处理器的适配器
     */
    private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

        /**
         * 子线程池相关属性
         */
        private final EventLoopGroup childGroup;
        private final ChannelHandler childHandler;
        private final Entry<ChannelOption<?>, Object>[] childOptions;
        private final Entry<AttributeKey<?>, Object>[] childAttrs;

        /**
         * 自动读取任务
         */
        private final Runnable enableAutoReadTask;

        /**
         * 创建一个ServerBootstrapAcceptor实例
         *
         * @param channel 主线程通道
         * @param childGroup 子事件循环组（工作线程）
         * @param childHandler 子通道处理器
         * @param childOptions 子通道选项
         * @param childAttrs 子通道属性
         */
        ServerBootstrapAcceptor(
                final Channel channel, EventLoopGroup childGroup, ChannelHandler childHandler,
                Entry<ChannelOption<?>, Object>[] childOptions, Entry<AttributeKey<?>, Object>[] childAttrs) {
            this.childGroup = childGroup;
            this.childHandler = childHandler;
            this.childOptions = childOptions;
            this.childAttrs = childAttrs;

            // Task which is scheduled to re-enable auto-read.
            // It's important to create this Runnable before we try to submit it as otherwise the URLClassLoader may
            // not be able to load the class because of the file limit it already reached.
            //
            // See https://github.com/netty/netty/issues/1328
            enableAutoReadTask = new Runnable() {
                @Override
                public void run() {
                    channel.config().setAutoRead(true);
                }
            };
        }

        /**
         * 监听可读事件
         *
         * 当客户端发送数据来到可读取时，触发此事件的执行
         *
         * @param ctx
         * @param msg
         */
        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 这个方法的功能，相当于AbstractBootstrap#initAndRegister()方法，就是初始化通道和注册

            final Channel child = (Channel) msg;
            logger.info("刚new出来的channel激活状态 => {}",child.isActive());
            logger.info("ServerBootstrapAcceptor处理器channelRead方法被调用，开始初始化并注册客户通道{}...",child.getClass().getSimpleName());

            logger.info("开始为客户端通道添加处理器childHandler...");
            // 为子通道添加处理器：此时会真正把处理业务逻辑的处理器加如到pipeline中
            child.pipeline().addLast(childHandler);
            logger.info("结束为客户端通道添加处理器childHandler...");

            // 为子通道添加通道选项和属性
            setChannelOptions(child, childOptions, logger);
            setAttributes(child, childAttrs);

            try {
                /**
                 * 将子通道注册到子事件循环相关联的Selector选择器上，
                 * 子事件循环即执行器执行添加处理器等任务时，将开始监听所有感兴趣的事件，此时将触发可读四化建
                 */
                logger.info("开始为客户端通道注册...");
                childGroup.register(child).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            forceClose(child, future.cause());
                        }
                    }
                });
                logger.info("结束为客户端通道注册...");
            } catch (Throwable t) {
                forceClose(child, t);
            }
            logger.info("结束初始化并注册客户通道...");
        }

        private static void forceClose(Channel child, Throwable t) {
            child.unsafe().closeForcibly();
            logger.warn("Failed to register an accepted channel: {}", child, t);
        }

        /**
         * 监听异常事件
         *
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final ChannelConfig config = ctx.channel().config();
            if (config.isAutoRead()) {
                // stop accept new connections for 1 second to allow the channel to recover
                // See https://github.com/netty/netty/issues/1328
                config.setAutoRead(false);
                ctx.channel().eventLoop().schedule(enableAutoReadTask, 1, TimeUnit.SECONDS);
            }
            // still let the exceptionCaught event flow through the pipeline to give the user
            // a chance to do something with it
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ServerBootstrap clone() {
        return new ServerBootstrap(this);
    }

    /**
     * Return the configured {@link EventLoopGroup} which will be used for the child channels or {@code null}
     * if non is configured yet.
     *
     * @deprecated Use {@link #config()} instead.
     */
    @Deprecated
    public EventLoopGroup childGroup() {
        return childGroup;
    }

    final ChannelHandler childHandler() {
        return childHandler;
    }

    final Map<ChannelOption<?>, Object> childOptions() {
        synchronized (childOptions) {
            return copiedMap(childOptions);
        }
    }

    final Map<AttributeKey<?>, Object> childAttrs() {
        return copiedMap(childAttrs);
    }

    @Override
    public final ServerBootstrapConfig config() {
        return config;
    }
}
