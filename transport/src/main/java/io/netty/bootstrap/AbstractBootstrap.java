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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractBootstrap，抽象引导
 * 它是一个帮助者类即辅助类，使引导一个通道Channel变的容易。它通过支持链式方法调用来提供一个简单的方式去配置抽象引导类。
 * 当不在一个服务器引导上下文中使用时，对于无连接传输比如数据报（UDP），bind()系列方法会非常有用
 *
 * support 支持;拥护;鼓励;帮助;援助;资助;赞助
 *
 * {@link AbstractBootstrap} is a helper class that makes it easy to bootstrap a {@link Channel}. It support
 * method-chaining to provide an easy way to configure the {@link AbstractBootstrap}.
 *
 * <p>When not used in a {@link ServerBootstrap} context, the {@link #bind()} methods are useful for connectionless
 * transports such as datagram (UDP).</p>
 */
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {
    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractBootstrap.class);

    @SuppressWarnings("unchecked")
    static final Map.Entry<ChannelOption<?>, Object>[] EMPTY_OPTION_ARRAY = new Map.Entry[0];
    @SuppressWarnings("unchecked")
    static final Map.Entry<AttributeKey<?>, Object>[] EMPTY_ATTRIBUTE_ARRAY = new Map.Entry[0];

    volatile EventLoopGroup group;
    @SuppressWarnings("deprecation")
    private volatile ChannelFactory<? extends C> channelFactory;
    private volatile SocketAddress localAddress;

    // The order in which ChannelOptions are applied is important they may depend on each other for validation
    // purposes.
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> attrs = new ConcurrentHashMap<AttributeKey<?>, Object>();
    private volatile ChannelHandler handler;

    AbstractBootstrap() {
        // Disallow extending from a different package.
    }

    AbstractBootstrap(AbstractBootstrap<B, C> bootstrap) {
        group = bootstrap.group;
        channelFactory = bootstrap.channelFactory;
        handler = bootstrap.handler;
        localAddress = bootstrap.localAddress;
        synchronized (bootstrap.options) {
            options.putAll(bootstrap.options);
        }
        attrs.putAll(bootstrap.attrs);
    }

    /**
     * 设置{@link EventLoopGroup}参数
     * 用于处理即将创建的通道所发生的所有事件
     *
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-created
     * {@link Channel}
     */
    public B group(EventLoopGroup group) {
        // 非null检查
        ObjectUtil.checkNotNull(group, "group");

        // 参数重复设置检查：如果当前的group参数已经设置，将非法状态异常IllegalStateException
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }

        // 赋值
        this.group = group;

        // 返回自己，实现链式调用
        return self();
    }

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }

    /**
     * 设置通道{@link Channel}的类型
     * 使用这个方法设置将创建的通道 {@link Channel}的类型。如果你的通道{@link Channel}实现类没有无参构造函数，可以使用{@link #channelFactory(io.netty.channel.ChannelFactory)}方法。
     * 实际上这个方法使用了指定的通道类型创建了一个指定类型的通道工厂
     *
     * 客户端和服务端引导配置的套接字类型不相同
     *
     * The {@link Class} which is used to create {@link Channel} instances from.
     * You either use this or {@link #channelFactory(io.netty.channel.ChannelFactory)} if your
     * {@link Channel} implementation has no no-args constructor.
     */
    public B channel(Class<? extends C> channelClass) {
        // 使用指定的通道类型创建一个对应的通道工厂：该工厂使用指定通道类型的无参构造函数反射创建通道对象
        logger.info("设置server接受器事件循环组acceptorGroup的通道类型{}...",channelClass.getSimpleName());
        return channelFactory(new ReflectiveChannelFactory<C>(
                ObjectUtil.checkNotNull(channelClass, "channelClass")
        ));
    }

    /**
     * 设置通道工厂channelFactory参数
     * 此方法已启用，建议使用 {@link #channelFactory(io.netty.channel.ChannelFactory)}方法代替
     *
     * @deprecated Use {@link #channelFactory(io.netty.channel.ChannelFactory)} instead.
     */
    @Deprecated
    public B channelFactory(ChannelFactory<? extends C> channelFactory) {
        // channelFactory非null检查
        ObjectUtil.checkNotNull(channelFactory, "channelFactory");

        // 重复设置检查
        if (this.channelFactory != null) {
            throw new IllegalStateException("channelFactory set already");
        }

        // 赋值
        this.channelFactory = channelFactory;

        // 返回自己，实现链式调用
        return self();
    }

    /**
     * 设置通道工厂channelFactory参数
     * 当调用{@link #bind()}系列方法或子类{@link Bootstrap#connect()}系列方法时（最终都是在{@link #initAndRegister()}方法），
     * 该通道工厂{@link io.netty.channel.ChannelFactory}将用来创建通道{@link Channel}实例。
     * 这个方法只有当你没有办法通过{@link #channel(Class)}指定的通道类型时使用，比如你的通道类比较复杂或没有无参构造函数。
     * 如果你的{@link Channel}实现有一个无参的构造函数，强烈建议你使用{@link #channel(Class)}以简化你的编码。
     *
     * {@link io.netty.channel.ChannelFactory} which is used to create {@link Channel} instances from
     * when calling {@link #bind()}. This method is usually only used if {@link #channel(Class)}
     * is not working for you because of some more complex needs. If your {@link Channel} implementation
     * has a no-args constructor, its highly recommend to just use {@link #channel(Class)} to
     * simplify your code.
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public B channelFactory(io.netty.channel.ChannelFactory<? extends C> channelFactory) {
        return channelFactory((ChannelFactory<C>) channelFactory);
    }

    /**
     * 设置本地主机地址参数localAddress
     *
     * The {@link SocketAddress} which is used to bind the local "end" to.
     */
    public B localAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return self();
    }

    /**
     * 设置本地主机地址参数localAddress
     *
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(int inetPort) {
        return localAddress(new InetSocketAddress(inetPort));
    }

    /**
     * 设置本地主机地址参数localAddress
     *
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(String inetHost, int inetPort) {
        return localAddress(SocketUtils.socketAddress(inetHost, inetPort));
    }

    /**
     * 设置本地主机地址参数localAddress
     *
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(InetAddress inetHost, int inetPort) {
        return localAddress(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * 设置通道{@link Channel}选项{@link ChannelOption}。
     * 允许指定一个在创建时用于通道{@link Channel}实例的通道选项{@link ChannelOption}。设置 {@code value}值为{@code null}，可以删除之前设置的选项值。
     *
     * specify 指定
     *
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they got
     * created. Use a value of {@code null} to remove a previous set {@link ChannelOption}.
     */
    public <T> B option(ChannelOption<T> option, T value) {
        // 键option非null检查
        ObjectUtil.checkNotNull(option, "option");

        // 同步锁
        synchronized (options) {
            // 如果值为null，删除选项
            if (value == null) {
                options.remove(option);
            }
            // 值不为null，则将选项放入选项集合options中
            else {
                options.put(option, value);
            }
        }

        return self();
    }

    /**
     * 设置通道{@link Channel}初始属性参数
     *
     * 允许为新创建的通道{@link Channel}指定一个初始属性。如果值{@code value}为{@code null}，对应{@code key}的属性将被删除。
     * Allow to specify an initial attribute of the newly created {@link Channel}.  If the {@code value} is
     * {@code null}, the attribute of the specified {@code key} is removed.
     */
    public <T> B attr(AttributeKey<T> key, T value) {
        // 键key的非null检查
        ObjectUtil.checkNotNull(key, "key");

        // 如果值为null，将删除对应key的属性
        if (value == null) {
            attrs.remove(key);
        }
        // 值不为null，则将选项放入属性集合attrs中
        else {
            attrs.put(key, value);
        }

        return self();
    }

    /**
     * 验证group和channelFactory参数。子类可以覆盖，但需要调用父类方法。
     * 客户端子类Bootstrap：额外验证handler
     * 服务端子类ServerBootstrap：额外验证childHandler、childGroup
     *
     * Validate all the parameters. Sub-classes may override this, but should
     * call the super method in that case.
     */
    public B validate() {
        // 检查循环事件组是否已经配置，即线程池
        if (group == null) {
            throw new IllegalStateException("group not set");
        }

        // 检查通道类型或通道工厂是否已经配置
        if (channelFactory == null) {
            throw new IllegalStateException("channel or channelFactory not set");
        }

        return self();
    }

    /**
     * Returns a deep clone of this bootstrap which has the identical configuration.  This method is useful when making
     * multiple {@link Channel}s with similar settings.  Please note that this method does not clone the
     * {@link EventLoopGroup} deeply but shallowly, making the group a shared resource.
     */
    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public abstract B clone();

    /**
     * 创建一个通道{@link Channel}以及将它注册到事件循环{@link EventLoop}中
     *
     * Create a new {@link Channel} and register it with an {@link EventLoop}.
     */
    public ChannelFuture register() {
        // 验证必须参数，父类即当前类验证group、channelFactory，子类会覆盖此方法：
        // Bootstrap：额外验证handler
        // ServerBootstrap：额外验证childHandler、childGroup
        validate();

        // 初始化（创建channel，初始化其它参数）并（将channel）注册（到EventLoop）
        return initAndRegister();
    }

    /**
     * 创建一个新的通道{@link Channel}并绑定它
     *
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind() {
        // 验证必须参数，父类即当前类验证group、channelFactory，子类会覆盖此方法：
        // Bootstrap：额外验证handler
        // ServerBootstrap：额外验证childHandler、childGroup
        validate();

        // 验证本地主机套接字地址
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            throw new IllegalStateException("localAddress not set");
        }

        logger.info("开始绑定本地主机地址...");

        // 指定本地套接字接口，创建通道并绑定
        return doBind(localAddress);
    }

    /**
     * 指定本地主机通讯端口，创建一个新的通道{@link Channel}并绑定它
     *
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }

    /**
     * 指定本地主机（网卡）地址和端口，创建一个新的通道{@link Channel}并绑定它
     *
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(SocketUtils.socketAddress(inetHost, inetPort));
    }

    /**
     * 指定本地主机（网卡）地址和端口，创建一个新的通道{@link Channel}并绑定它
     *
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(InetAddress inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * 指定本地主机套接字地址，创建一个新的通道{@link Channel}并绑定它
     *
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(SocketAddress localAddress) {
        // 验证参数
        validate();

        logger.info("开始绑定本地主机地址...");
        // 指定本地主机套接字接口，创建通道并绑定
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }

    /**
     * 绑定指定的本地主机套接字地址
     * 创建并初始化服务器套接字通道，然后将其注册到一个事件循环EventLoop中，并开始监听OP_ACCEPT事件，等待客户端连接
     *
     * @param localAddress 本地主机套接字地址
     * @return 返回一个通道异步回调实例
     */
    private ChannelFuture doBind(final SocketAddress localAddress) {
        // 创建并初始化通道channel，并将通道channel注册到EventLoop/EventExecutor关联的选择器Selector上
        final ChannelFuture regFuture = initAndRegister();

        // 获取创建的通道实例channel
        final Channel channel = regFuture.channel();
        // 通道注册失败（失败原因cause()不为null，说明注册已失败），直接返回失败的注册回调regFuture
        if (regFuture.cause() != null) {
            return regFuture;
        }

        // 通道注册完成（一般认为注册成功）
        if (regFuture.isDone()) {
            // 程序运行到这里说明注册操作完成并且注册成功
            // At this point we know that the registration was complete and successful.

            // 新建一个绑定通道异步回调promise
            ChannelPromise promise = channel.newPromise();

            // 执行通道绑定操作
            doBind0(regFuture, channel, localAddress, promise);

            // 返回绑定通道的异步回调
            return promise;
        }
        // 未完成注册操作，成败状态未知，可能失败也可能没有失败（注册中，但还没成功)
        else {
            // 通道注册的异步回调几乎总是已经执行(done)了，这里主要是以防万一
            // Registration future is almost always fulfilled already, but just in case it's not.

            // 创建一个待注册异步回调promise
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);

            // 添加注册操作监听器，监听注册操作完成，获取注册操作结果
            regFuture.addListener(new ChannelFutureListener() {
                /**
                 * 操作完成时触发此方法
                 *
                 * @param future  the source {@link io.netty.util.concurrent.Future} which called this callback
                 * @throws Exception
                 */
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // 获取操作失败原因
                    Throwable cause = future.cause();

                    // 如果存在操作失败原因，说明通道注册操作失败
                    if (cause != null) {
                        // 在EventLoop上注册失败，设置注册异步回调promise为失败状态避免当我们尝试访问通道的EventLoop时导致非法状态异常
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    }
                    // 通道注册操作成功
                    else {
                        // 注册成功，所以设置用于正确的执行器
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        // 执行通道绑定操作
                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });

            return promise;
        }
    }

    /**
     * 初始化和注册
     * 创建通道，初始化引导对象，并将通道注册到事件循环对象中
     *
     * @return 返回通道注册的异步回调对象ChannelFuture
     */
    final ChannelFuture initAndRegister() {
        // 声明channel变量
        Channel channel = null;

        // 创建通道channel并初始化引导AbstractBootstrap具体子类实例
        try {
            // 使用绑定了通道类型的通道工厂对象channelFactory创建对应通道类型的通道对象实例
            logger.info("使用通道工厂创建通道...");
            channel = channelFactory.newChannel();
            logger.info("channelFactory.newChannel()创建的通道状态：{}...",channel.isActive());
            logger.info("通道创建完毕{}...",channel.getClass().getSimpleName());

            // 初始化引导对象AbstractBootstrap具体子类实例：客户端和服务端将开始有不同的实现
            logger.info("开始初始化通道...");
            init(channel);
            logger.info("结束初始化通道...");
        } catch (Throwable t) {
            // 创建通道或初始化引导实例出现异常
            // 如果channel创建成功（也有创建失败为null的可能如果newChannel崩溃比如套接字异常等引起，所以这里需要对channel进行非null判断）
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                // 调用Unsafe对象的closeForcibly方法立即关闭通道，该方法不会触发任何事件，在还未注册成功(或注册失败)时关闭不会影响其它程序
                channel.unsafe().closeForcibly();

                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                // 由于通道尚未注册，需要强制使用全局事件执行器GlobalEventExecutor实例来创建一个失败的异步回调Promise返回
                // 这里传入成功创建的通道，表示通道有创建成功
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }

            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            // 由于通道尚未注册，需要强制使用全局事件执行器GlobalEventExecutor实例来创建一个失败的异步回调Promise返回
            // 这里传入一个表示失败的通道对象，表示通道没有创建成功
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        logger.info("开始注册通道...");
        // 将通道channel注册到EventLoopGroup中的EventLoop中关联的Selector上
        ChannelFuture regFuture = config().group().register(channel);
        logger.info("结束注册通道...");
        // 如果(同步)注册失败，关闭通道
        if (regFuture.cause() != null) {
            // 如果通道channel已经注册，则调用channel的关闭方法close关闭通道，此方法会触发通道事件
            if (channel.isRegistered()) {
                channel.close();
            }
            // 如果通道channel未注册，则调用Unsafe的closeForcibly()关闭通道，此方法不会触发通道事件
            else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.

        // 程序运行到这里，假如注册的异步回调promise对象即regFuture没有失败，它是以下的情况之一：
        // 1）假设我们尝试了从事件循环中注册,此时注册已经完成。即，现在尝试bind()或connect()方法是安全的，因为通道channel已经注册。
        // 2) 假设我们尝试了从其它线程中注册,此时注册请求已经被成功添加到稍后执行的事件循环的任务队列中。即，现在尝试bind()或connect()方法是安全的：
        //    因为bind()或connect()方法将会在调度注册任务被执行后执行
        //    因为注册register()、绑定bind()和连接connect()方法都是绑定在相同的线程上
        //
        // attempt 尝试、企图
        // i.e. 也就是，即，亦即

        /** 个人注解 **/
        // 程序运行到这里，如果注册回调regFuture没有失败，那么此时调用绑定方法bind()或连接方法connect()是安全的，
        // 因为创建注册回调regFuture对象的register()方法和bind()方法、connect()方法都是绑定在同一个线程上的：
        // 通道channel注册成功，调用bind()、connect()方法将正常，
        // 如果失败，因为是同一个线程，其调用也是按顺序的，所以失败状态也会传递给后面的两个方法（后面两个方法会被中断或是会正常捕获到失败的回调?) TODO 待阅读源码验证

        // 返回通道注册异步回调对象regFuture
        return regFuture;
    }

    /**
     * 初始化Bootstrap具体子类实例
     *
     * 由于客户端和服务端初始化的业务需求不一样，需要各自分别实现，这里使用抽象模板方法，满足需求
     *
     * @param channel (主线程)通道
     * @throws Exception
     */
    abstract void init(Channel channel) throws Exception;

    /**
     * 注册通道后，绑定本地主机和通道
     *
     * @param regFuture 注册通道异步回调对象
     * @param channel 关联的通道
     * @param localAddress 本地主机地址
     * @param promise 绑定通道异步回调对象
     */
    private static void doBind0(
            final ChannelFuture regFuture, final Channel channel,
            final SocketAddress localAddress, final ChannelPromise promise) {

        //这个方法在ChannelInboundHandler#channelRegistered()触发前被调用。
        // 给用户的处理器handlers一个机会设置管道自身channelRegistered()实现。
        // This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
        // the pipeline in its channelRegistered() implementation.

        // 创建并启动新线程执行绑定通道和本地主机端口的任务
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                // 如果通道注册成功，则绑定本地主机和通道
                if (regFuture.isSuccess()) {
                    // 绑定通道和本地主机地址，并监听绑定结果，如果绑定失败，将关闭此通道
                    logger.info("调用channel.bind(localAddress, promise)方法...");
                    channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                }
                // 如果通道注册失败，不会进行通道绑定操作，直接认为通道绑定失败，
                else {
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

    /**
     * 设置处理器handler参数
     * handler用于服务接收到的请求，即处理用户的请求（入站信息）
     *
     * the {@link ChannelHandler} to use for serving the requests.
     */
    public B handler(ChannelHandler handler) {
        this.handler = ObjectUtil.checkNotNull(handler, "handler");
        return self();
    }

    /**
     * 返回group参数
     *
     * Returns the configured {@link EventLoopGroup} or {@code null} if non is configured yet.
     *
     * @deprecated Use {@link #config()} instead.
     */
    @Deprecated
    public final EventLoopGroup group() {
        return group;
    }

    /**
     * 返回引导配置
     *
     * Returns the {@link AbstractBootstrapConfig} object that can be used to obtain the current config
     * of the bootstrap.
     */
    public abstract AbstractBootstrapConfig<B, C> config();

    /**
     * 创建空白的选项集合数组
     *
     * @return
     */
    final Map.Entry<ChannelOption<?>, Object>[] newOptionsArray() {
        synchronized (options) {
            return options.entrySet().toArray(EMPTY_OPTION_ARRAY);
        }
    }

    /**
     * 返回通道选项集合
     *
     * @return
     */
    final Map<ChannelOption<?>, Object> options0() {
        return options;
    }

    /**
     * 返回通道属性集合
     *
     * @return
     */
    final Map<AttributeKey<?>, Object> attrs0() {
        return attrs;
    }

    /**
     * 返回本地主机地址
     *
     * @return
     */
    final SocketAddress localAddress() {
        return localAddress;
    }

    /**
     * 返回通道工厂
     *
     * @return
     */
    @SuppressWarnings("deprecation")
    final ChannelFactory<? extends C> channelFactory() {
        return channelFactory;
    }

    /**
     * 返回处理器
     *
     * @return
     */
    final ChannelHandler handler() {
        return handler;
    }

    /**
     * 返回通道选项集合拷贝
     *
     * @return
     */
    final Map<ChannelOption<?>, Object> options() {
        synchronized (options) {
            return copiedMap(options);
        }
    }

    /**
     * 返回通道属性集合拷贝
     *
     * @return
     */
    final Map<AttributeKey<?>, Object> attrs() {
        return copiedMap(attrs);
    }

    /**
     * 拷贝映射集
     *
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    static <K, V> Map<K, V> copiedMap(Map<K, V> map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<K, V>(map));
    }

    /**
     * 设置通道属性集
     *
     * @param channel 通道
     * @param attrs 属性值集合
     */
    static void setAttributes(Channel channel, Map.Entry<AttributeKey<?>, Object>[] attrs) {
        for (Map.Entry<AttributeKey<?>, Object> e: attrs) {
            @SuppressWarnings("unchecked")
            AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
            channel.attr(key).set(e.getValue());
        }
    }

    /**
     * 设置通道选项集
     *
     * @param channel 通道
     * @param options 选项集合
     * @param logger 日志
     */
    static void setChannelOptions(
            Channel channel, Map.Entry<ChannelOption<?>, Object>[] options, InternalLogger logger) {
        for (Map.Entry<ChannelOption<?>, Object> e: options) {
            // 逐个设置通道选项
            setChannelOption(channel, e.getKey(), e.getValue(), logger);
        }
    }

    /**
     * 设置通道选项
     *
     * @param channel 通道
     * @param option 选项
     * @param value 选项值
     * @param logger 日志
     */
    @SuppressWarnings("unchecked")
    private static void setChannelOption(
            Channel channel, ChannelOption<?> option, Object value, InternalLogger logger) {
        try {
            if (!channel.config().setOption((ChannelOption<Object>) option, value)) {
                logger.warn("Unknown channel option '{}' for channel '{}'", option, channel);
            }
        } catch (Throwable t) {
            logger.warn(
                    "Failed to set channel option '{}' with value '{}' for channel '{}'", option, value, channel, t);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
            .append(StringUtil.simpleClassName(this))
            .append('(').append(config()).append(')');
        return buf.toString();
    }

    /**
     * 待注册承诺
     */
    static final class PendingRegistrationPromise extends DefaultChannelPromise {

        /**
         * 是否注册成功
         * 一旦注册成功，将设置正确的事件执行器EventExecutor。否则它将一直为null，因此GlobalEventExecutor.INSTANCE将会被用于通知。
         */
        // Is set to the correct EventExecutor once the registration was successful. Otherwise it will
        // stay null and so the GlobalEventExecutor.INSTANCE will be used for notifications.
        private volatile boolean registered;

        PendingRegistrationPromise(Channel channel) {
            super(channel);
        }

        /**
         * 更新注册状态为已注册
         */
        void registered() {
            registered = true;
        }

        @Override
        protected EventExecutor executor() {
            // 注册成功,返会设置的事件执行器实例
            if (registered) {
                // If the registration was a success executor is set.
                //
                // See https://github.com/netty/netty/issues/2586
                return super.executor();
            }

            // 注册失败，只能返回全局的事件执行器实例
            // The registration failed so we can only use the GlobalEventExecutor as last resort to notify.
            return GlobalEventExecutor.INSTANCE;
        }
    }
}
