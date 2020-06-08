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
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.NameResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 给客户端使用的一个为了更容易启动（或连接本地到远程主机的）{@link Channel}的引导类{@link Bootstrap}
 *
 * A {@link Bootstrap} that makes it easy to bootstrap a {@link Channel} to use
 * for clients.
 *
 * 在结合无连接传输比如数据报协议UDP的应用场景下，{@link #bind()}系列方法显得非常有用。如果是常规的TCP连接，请使用提供的{@link #connect()}系列方法。
 * 即对于客户端而言，UDP应用场景使用bind方法绑定（本地或局域网、远程）主机，TCP应用场景使用connect方法连接（本地或局域网、远程）主机即服务器
 *
 * combination with sth 结合某事
 *
 * <p>The {@link #bind()} methods are useful in combination with connectionless transports such as datagram (UDP).
 * For regular TCP connections, please use the provided {@link #connect()} methods.</p>
 */
public class Bootstrap extends AbstractBootstrap<Bootstrap, Channel> {

    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Bootstrap.class);

    /**
     * 缺省的（主机）地址解析器组
     */
    private static final AddressResolverGroup<?> DEFAULT_RESOLVER = DefaultAddressResolverGroup.INSTANCE;

    /**
     * 引导程序配置
     */
    private final BootstrapConfig config = new BootstrapConfig(this);

    /**
     * （主机Socket）地址解析器组
     */
    @SuppressWarnings("unchecked")
    private volatile AddressResolverGroup<SocketAddress> resolver =
            (AddressResolverGroup<SocketAddress>) DEFAULT_RESOLVER;

    /**
     * 远程主机Socket地址
     */
    private volatile SocketAddress remoteAddress;

    /**
     * 初始化一个空的引导类
     */
    public Bootstrap() { }

    private Bootstrap(Bootstrap bootstrap) {
        super(bootstrap);
        resolver = bootstrap.resolver;
        remoteAddress = bootstrap.remoteAddress;
    }

    /**
     * 设置地址解析器{@link NameResolver}
     *
     * Sets the {@link NameResolver} which will resolve the address of the unresolved named address.
     *
     * @param resolver the {@link NameResolver} for this {@code Bootstrap}; may be {@code null}, in which case a default
     *                 resolver will be used 指定给{@code Bootstrap}的地址解析器，可以为{@code null}，如果为{@code null}，则将使用缺省地址解析器
     *
     * @see io.netty.resolver.DefaultAddressResolverGroup
     */
    @SuppressWarnings("unchecked")
    public Bootstrap resolver(AddressResolverGroup<?> resolver) {
        this.resolver = (AddressResolverGroup<SocketAddress>) (resolver == null ? DEFAULT_RESOLVER : resolver);
        return this;
    }

    /**
     * 设置一旦{@link #connect()}方法被调用时，需要连接的{@link SocketAddress}
     *
     * The {@link SocketAddress} to connect to once the {@link #connect()} method
     * is called.
     */
    public Bootstrap remoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    /**
     * 设置一旦{@link #connect()}方法被调用时，需要连接的远程主机地址字符串和端口。参考{@link #remoteAddress(SocketAddress)}
     *
     * @see #remoteAddress(SocketAddress)
     */
    public Bootstrap remoteAddress(String inetHost, int inetPort) {
        remoteAddress = InetSocketAddress.createUnresolved(inetHost, inetPort);
        return this;
    }

    /**
     * 设置一旦{@link #connect()}方法被调用时，需要连接的网络主机地址和端口。参考{@link #remoteAddress(SocketAddress)}
     *
     * @see #remoteAddress(SocketAddress)
     */
    public Bootstrap remoteAddress(InetAddress inetHost, int inetPort) {
        remoteAddress = new InetSocketAddress(inetHost, inetPort);
        return this;
    }

    /**
     * 连接一个通道{@link Channel}到远程对等方（远程服务器、远程主机）
     *
     * Connect a {@link Channel} to the remote peer.
     */
    public ChannelFuture connect() {
        // 在连接远程主机之前，先验证必需配置的参数是否已经设置，如果没有设置，将抛出异常
        // 包括group、channelFactory、handler等组件，主要是对这些参数做非null检查
        validate();

        // 检查远程主机地址remoteAddress参数是否已经设置
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            throw new IllegalStateException("remoteAddress not set");
        }

        // 解析要连接的远程主机并绑定本机通信地址，然后开始连接
        return doResolveAndConnect(remoteAddress, config.localAddress());
    }

    /**
     * 连接一个通道{@link Channel}到远程主机
     *
     * Connect a {@link Channel} to the remote peer.
     */
    public ChannelFuture connect(String inetHost, int inetPort) {
        return connect(InetSocketAddress.createUnresolved(inetHost, inetPort));
    }

    /**
     * 连接一个通道{@link Channel}到远程主机
     *
     * Connect a {@link Channel} to the remote peer.
     */
    public ChannelFuture connect(InetAddress inetHost, int inetPort) {
        return connect(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * 连接一个通道{@link Channel}到远程主机
     *
     * Connect a {@link Channel} to the remote peer.
     */
    public ChannelFuture connect(SocketAddress remoteAddress) {
        // remoteAddress参数非null检查
        ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");

        // 验证group、channelFactory、handler
        validate();

        // 解析要连接的远程主机并绑定本机通信地址，然后开始连接
        return doResolveAndConnect(remoteAddress, config.localAddress());
    }

    /**
     * 连接一个通道{@link Channel}到远程主机
     *
     * Connect a {@link Channel} to the remote peer.
     */
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        // remoteAddress参数非null检查
        ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");

        // 验证group、channelFactory、handler
        validate();

        // 解析要连接的远程主机并绑定本机通信地址，然后开始连接
        return doResolveAndConnect(remoteAddress, localAddress);
    }

    /**
     * 解析远程主机并绑定本机通信地址，然后开始连接
     *
     * @param remoteAddress 远程主机地址
     * @param localAddress  本地主机地址
     * @return
     *
     * @see #connect()
     */
    private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
        /**
         * 1.创建并初始化通道Channel，将通道Channel和EventLoop关联，然后将通道Channel注册到EventLoop中关联的选择器Selector
         * 这个过程当中，会将处理器handler绑定到对应的channel中pipeline中
         */
        final ChannelFuture regFuture = initAndRegister();

        /**
         * 2.获取通道，解析并连接远程主机
         */
        final Channel channel = regFuture.channel();

        // 通道注册操作完成，检查注册状态
        if (regFuture.isDone()) {
            // 如果注册不成功即失败，直接返回失败状态的通道注册异步回调regFuture
            if (!regFuture.isSuccess()) {
                return regFuture;
            }

            // 注册成功，执行解析和连接远程主机
            return doResolveAndConnect0(channel, remoteAddress, localAddress, channel.newPromise());
        }
        else {
            // 通道注册的异步回调几乎总是已经执行(done)了，这里主要是以防万一
            // almost always 几乎总是
            // fulfilled 实现，执行，履行
            // Registration future is almost always fulfilled already, but just in case it's not.
            // 创建可写的异步回调执行【待注册异步回调执行promise】
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);

            // 为通道注册异步回调结果添加监听器，内部绑定promise，监听注册完成事件
            regFuture.addListener(new ChannelFutureListener() {
                /**
                 * 处理注册完成事件：注册完成，将触发此方法
                 *
                 * @param future  the source {@link Future} which called this callback 调用此回调方法的源{@link Future}
                 * @throws Exception
                 */
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // 直接获取注册失败原因
                    // Directly obtain the cause and do a null check so we only need one volatile read in case of a
                    // failure.
                    Throwable cause = future.cause();

                    // 如果失败原因不为null，即存在失败原因，说明注册失败
                    if (cause != null) {
                        // 注册到EventLoop失败，更新待注册承诺的状态为失败，并传递失败原因
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // 注册成功
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        // 更新注册状态
                        promise.registered();

                        // 执行解析和连接远程主机
                        doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
                    }
                }
            });

            return promise;
        }
    }

    /**
     * channel注册成功，解析和连接到远程主机
     *
     * @param channel 连接到远程主机的的通道（本地对象）
     * @param remoteAddress 远程主机地址
     * @param localAddress 本地主机地址
     * @param promise 异步回调执行对象
     * @return
     */
    private ChannelFuture doResolveAndConnect0(final Channel channel, SocketAddress remoteAddress,
                                               final SocketAddress localAddress, final ChannelPromise promise) {
        try {
            // 1.获取通道关联的事件循环/事件执行器
            // EventLoop的实例也是EventExecutor实例，下面eventLoop将会被传入到地址解析器组以获取关联的地址解析器
            final EventLoop eventLoop = channel.eventLoop();

            // 2.地址解析器
            AddressResolver<SocketAddress> resolver;
            try {
                // 使用地址解析器组获取地址解析器
                resolver = this.resolver.getResolver(eventLoop);
            } catch (Throwable cause) {
                // 如果获取过程出现异常，关闭通道
                channel.close();

                // 异步回调执行状态设置为失败并返回
                return promise.setFailure(cause);
            }

            // 3.多种情况下分别尝试连接远程主机
            // 3.1如果解析器不支持该解析远程主机地址，或已经解析过该远程主机地址（解析过意味着该地址是解析器支持解析）
            if (!resolver.isSupported(remoteAddress) || resolver.isResolved(remoteAddress)) {

                // 解析器不知道如何解析指定的远程主机地址 或 已经解析过该远程主机地址，依然尝试连接?
                // Resolver has no idea about what to do with the specified remote address or it's resolved already.
                doConnect(remoteAddress, localAddress, promise);

                // 返回承诺：成败未知，取决于将来连接的最终结果
                return promise;
            }

            // 3.2 解析器未解析过的地址（对于Bootstrap对象来说时新的主机地址）或已经解析过且是解析器支持的远程主机地址
            // 解析远程主机地址
            final Future<SocketAddress> resolveFuture = resolver.resolve(remoteAddress);

            // 远程主机地址解析操作【同步】完成，判断解析成功与否
            if (resolveFuture.isDone()) {
                // 获取解析失败原因
                final Throwable resolveFailureCause = resolveFuture.cause();

                // 如果存在解析失败原因，那说明解析失败，则关闭通道，并设置承诺状态为失败
                if (resolveFailureCause != null) {
                    // 解析失败，关闭通道
                    // Failed to resolve immediately
                    channel.close();

                    // 设置承诺为失败状态
                    promise.setFailure(resolveFailureCause);
                }
                else {
                    // 解析成功，连接远程主机
                    // Succeeded to resolve immediately; cached? (or did a blocking lookup)
                    doConnect(resolveFuture.getNow(), localAddress, promise);
                }

                // 返回承诺
                return promise;
            }

            // 【远程主机地址解析完成还未完成】，可能成功也可能失败，
            // 添加监听器监听最终解析操作的结果，然后做下一步的处理（失败则设置异步回调的promise状态为失败，成功则接远程主机）
            // Wait until the name resolution is finished.
            resolveFuture.addListener(new FutureListener<SocketAddress>() {
                /**
                 * 解析完成时触发此方法
                 *
                 * @param future  the source {@link Future} which called this callback
                 * @throws Exception
                 */
                @Override
                public void operationComplete(Future<SocketAddress> future) throws Exception {
                    // 解析失败
                    if (future.cause() != null) {
                        //关闭通道
                        channel.close();

                        // 设置承诺未失败状态
                        promise.setFailure(future.cause());
                    }
                    else {
                        // 解析成功，连接远程主机
                        doConnect(future.getNow(), localAddress, promise);
                    }
                }
            });
        }
        catch (Throwable cause) {
            // 发生其它异常，承诺设置为失败状态
            promise.tryFailure(cause);
        }

        // 返回承诺对象，其状态（失败或成功）由【远程主机地址解析完成还未完成】的情况下由resolveFuture添加的监听器的回调更新
        return promise;
    }

    /**
     * 尝试连接远程主机
     *
     * @param remoteAddress
     * @param localAddress
     * @param connectPromise
     */
    private static void doConnect(
            final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise connectPromise) {

        // 这个方法在channelRegistered()方法被触发之前调用(即在通道注册成功但还没有标识为已注册之前调用此方法?）。给用户的处理器handlers一个机会去设置管道实现。
        // This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
        // the pipeline in its channelRegistered() implementation.

        // 获取通道
        final Channel channel = connectPromise.channel();

        // 创建一个线程任务，连接远程主机，并设置监听器，连接失败时关闭通道
        channel.eventLoop().execute(new Runnable() {
            /**
             * 定义运行的任务：连接远程主机和设置监听器
             */
            @Override
            public void run() {

                // 如果没有指定本地主机地址，则调用无本地主机地址的连接方法连接远程主机
                if (localAddress == null) {
                    channel.connect(remoteAddress, connectPromise);
                }
                // 如果指定本地主机地址，则调用有本地主机地址的连接方法连接远程主机
                else {
                    channel.connect(remoteAddress, localAddress, connectPromise);
                }

                // 为连接操作的异步回调connectPromise添加失败关闭的监听器，即连接失败，将关闭创建的channel
                connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        });
    }

    /**
     * 初始化客户端套接字通道
     * 在调用NIO底层API连接远程主机前，调用此方法
     *
     * @param channel 客户端套接字通道实例，比如NioSocketChannel实例
     */
    @Override
    @SuppressWarnings("unchecked")
    void init(Channel channel) {
        // 获取通道管道pipeline
        ChannelPipeline p = channel.pipeline();

        logger.debug("开始添加首个处理器...");
        // 将handler添加到pipeline中
        // 这是首次将处理器插入到pipeline中，注意此时的channel还未注册，将会创建一个PendingHandlerAddedTask任务，
        // 在注册通道时，该任务将会被执行
        p.addLast(config.handler());
        logger.debug("结束添加首个处理器...");

        // 设置ChannelOption
        setChannelOptions(channel, newOptionsArray(), logger);

        // 设置在通道中使用的自定义属性
        setAttributes(channel, attrs0().entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY));
    }

    /**
     * 参数验证
     *
     * @return
     */
    @Override
    public Bootstrap validate() {
        // 调用父类验证方法
        super.validate();

        // 如果没有配置处理器，抛出异常
        if (config.handler() == null) {
            throw new IllegalStateException("handler not set");
        }

        return this;
    }

    /**
     * 深度克隆
     *
     * @return
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Bootstrap clone() {
        return new Bootstrap(this);
    }


    /**
     * 深度克隆当前对象，除了指定的group外
     *
     * Returns a deep clone of this bootstrap which has the identical configuration except that it uses
     * the given {@link EventLoopGroup}. This method is useful when making multiple {@link Channel}s with similar
     * settings.
     */
    public Bootstrap clone(EventLoopGroup group) {
        Bootstrap bs = new Bootstrap(this);
        bs.group = group;
        return bs;
    }

    @Override
    public final BootstrapConfig config() {
        return config;
    }

    final SocketAddress remoteAddress() {
        return remoteAddress;
    }

    final AddressResolverGroup<?> resolver() {
        return resolver;
    }
}
