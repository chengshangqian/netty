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
package io.netty.channel.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopException;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.SelectStrategy;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.IntSupplier;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReflectionUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 非阻塞IO事件循环类，单线程事件循环类{@link SingleThreadEventLoop}的子类实现。
 * 它将通道{@link Channel}注册到选择器{@link Selector}，并在这些事件循环中的这些通道(the Channels)实现多路复用。
 *
 * 以任务的形式轮询Selector的事件，顾名思义，事件循环
 *
 * {@link SingleThreadEventLoop} implementation which register the {@link Channel}'s to a
 * {@link Selector} and so does the multi-plexing of these in the event loop.
 *
 */
public final class NioEventLoop extends SingleThreadEventLoop {

    /**
     * 内部日志类
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioEventLoop.class);

    /**
     * 清理间隔
     */
    private static final int CLEANUP_INTERVAL = 256; // XXX Hard-coded value, but won't need customization.

    /**
     * 关闭键设置优化，默认开启（即不关闭）
     */
    private static final boolean DISABLE_KEY_SET_OPTIMIZATION =
            SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);

    /**
     * 最小PREMATURE选择器返回数量?
     */
    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;

    /**
     * 选择器自动重建（重新构建）阈值或门槛
     */
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;

    /**
     * selectNow提供值：非阻塞选择结果返回提供者
     */
    private final IntSupplier selectNowSupplier = new IntSupplier() {
        @Override
        public int get() throws Exception {
            // 返回注册的selector的检查方法selectNow()结果
            return selectNow();
        }
    };

    // JDK NIO bug（空轮询CPU使用率100%的问题）的解决方法
    // Workaround for JDK NIO bug.
    //
    // See:
    // - http://bugs.sun.com/view_bug.do?bug_id=6427854
    // - https://github.com/netty/netty/issues/203
    static {
        final String key = "sun.nio.ch.bugLevel";
        final String bugLevel = SystemPropertyUtil.get(key);
        if (bugLevel == null) {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        System.setProperty(key, "");
                        return null;
                    }
                });
            } catch (final SecurityException e) {
                logger.debug("Unable to get/set System Property: " + key, e);
            }
        }

        /**
         * 获取选择器自动重建阈值系统属性
         */
        int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);

        /**
         * 如果门槛小于MIN_PREMATURE_SELECTOR_RETURNS即小于3，则赋值为0
         */
        if (selectorAutoRebuildThreshold < MIN_PREMATURE_SELECTOR_RETURNS) {
            selectorAutoRebuildThreshold = 0;
        }

        /**
         * 赋值选择器自动重建阈值，等于0或大于等于3
         * （ SELECTOR_AUTO_REBUILD_THRESHOLD == 0 || SELECTOR_AUTO_REBUILD_THRESHOLD >= 3)
         */
        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.noKeySetOptimization: {}", DISABLE_KEY_SET_OPTIMIZATION);
            logger.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", SELECTOR_AUTO_REBUILD_THRESHOLD);
        }
    }

    /**
     * NIO选择器{@link Selector}
     *
     * The NIO {@link Selector}.
     */
    private Selector selector;

    /**
     * 未经包装过的NIO选择器{@link Selector}
     */
    private Selector unwrappedSelector;

    /**
     * 可选择的SelectionKey集合
     */
    private SelectedSelectionKeySet selectedKeys;

    /**
     * Selector提供者：在windows系统中，默认是WindowsSelectorProvider
     */
    private final SelectorProvider provider;

    /**
     * 唤醒状态
     */
    private static final long AWAKE = -1L;

    /**
     * 无，没有设置唤醒时间
     */
    private static final long NONE = Long.MAX_VALUE;

    /**
     * 下一个/次唤醒的(纳米)时间，默认为AWAKE
     * AWAKE：-1 表示是唤醒的
     * NONE：等待状态，且没有设置唤醒时间（调度）
     * 其它值T：等待状态，但将在时间T被唤醒?
     */
    // nextWakeupNanos is:
    //    AWAKE            when EL is awake
    //    NONE             when EL is waiting with no wakeup scheduled
    //    other value T    when EL is waiting with wakeup scheduled at time T
    private final AtomicLong nextWakeupNanos = new AtomicLong(AWAKE);

    /**
     * 选择策略
     */
    private final SelectStrategy selectStrategy;

    /**
     * 子线程I/O任务处理时间占比
     */
    private volatile int ioRatio = 50;

    /**
     * 取消的SelectionKey数量
     */
    private int cancelledKeys;

    /**
     * 是否需要再次选择
     */
    private boolean needsToSelectAgain;

    /**
     * 创建一个NioEventLoop实例
     *
     * @param parent 父事件循环组
     * @param executor 主线程（事件）执行器
     * @param selectorProvider 选择器selector提供者
     * @param strategy 选择策略
     * @param rejectedExecutionHandler 拒绝执行处理器
     * @param queueFactory 事件循环任务队列工厂
     */
    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler,
                 EventLoopTaskQueueFactory queueFactory) {
        // 调用父类构造方法
        super(parent, executor, false, newTaskQueue(queueFactory), newTaskQueue(queueFactory),
                rejectedExecutionHandler);

        // 初始化selector提供者和选择策略
        this.provider = ObjectUtil.checkNotNull(selectorProvider, "selectorProvider");
        this.selectStrategy = ObjectUtil.checkNotNull(strategy, "selectStrategy");

        // 创建选择器二元类实例，内部包含了未经包装的NIO选择器Selector（unwrappedSelector）和可能经过包装的选择器Selector（selector）
        final SelectorTuple selectorTuple = openSelector();

        // 初始化selector，unwrappedSelector
        this.selector = selectorTuple.selector;
        this.unwrappedSelector = selectorTuple.unwrappedSelector;
    }

    /**
     * 创建任务队列
     *
     * @param queueFactory 事件循环任务队列工厂
     * @return
     */
    private static Queue<Runnable> newTaskQueue(
            EventLoopTaskQueueFactory queueFactory) {

        // 如果事件循环任务队列工厂为null，创建平台相关的任务队列
        if (queueFactory == null) {
            return newTaskQueue0(DEFAULT_MAX_PENDING_TASKS);
        }

        // 否则，使用队列工厂对象创建新的任务队列，指定最大挂起任务数未缺省最大挂起任务数(16 ~ Integer.MAX_VALUE)
        return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
    }

    /**
     * 选择器元组：二元选择器类，即内部包含了两个选择器实例
     */
    private static final class SelectorTuple {
        /**
         * 未包装的选择器
         */
        final Selector unwrappedSelector;

        /**
         * 选择器，可能经过包装
         */
        final Selector selector;

        /**
         * 初始化选择器二元类实例
         *
         * @param unwrappedSelector 未包装的选择器
         */
        SelectorTuple(Selector unwrappedSelector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = unwrappedSelector;
        }

        /**
         * 初始化选择器二元类实例，分别指定unwrappedSelector和selector
         *
         * @param unwrappedSelector
         * @param selector
         */
        SelectorTuple(Selector unwrappedSelector, Selector selector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = selector;
        }
    }

    /**
     * 创建选择器二元组，即打开选择器
     * 同时尝试优化JAVA NIO原生的选择键
     *
     * @return
     */
    private SelectorTuple openSelector() {
        // 原始未包装的Selector
        final Selector unwrappedSelector;

        try {
            // 获取JAVA NIO原生的selector
            unwrappedSelector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }

        // 如果没有开启选择键的优化，封装未经包装的原生selector到SelectorTuple二元组返回
        if (DISABLE_KEY_SET_OPTIMIZATION) {
            return new SelectorTuple(unwrappedSelector);
        }

        // 如果开启了选择键的优化，尝试获取选择器的实现类类型
        Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    return Class.forName(
                            "sun.nio.ch.SelectorImpl",
                            false,
                            PlatformDependent.getSystemClassLoader());
                } catch (Throwable cause) {
                    return cause;
                }
            }
        });

        // 如果没有获取到乐预期的类型，封装未经包装的原生selector到SelectorTuple二元组返回
        if (!(maybeSelectorImplClass instanceof Class) ||
            // ensure the current selector implementation is what we can instrument.
            !((Class<?>) maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass())) {
            if (maybeSelectorImplClass instanceof Throwable) {
                Throwable t = (Throwable) maybeSelectorImplClass;
                logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, t);
            }
            return new SelectorTuple(unwrappedSelector);
        }

        // 如果时预期中的选择器实现类
        // 转为类型
        final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;
        // 创建优化的选择器键集合，内部使用数组保存选择键，比较原始的集合迭代方式，使用数组效率更好
        final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();

        // 尝试进行优化的具体操作，也可能会抛出异常：
        // 直接将JAVA NIO原生选择器对象实例的selectedKeys和publicSelectedKeys成员实例替换为优化后的selectedKeySet
        Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
                    Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");

                    if (PlatformDependent.javaVersion() >= 9 && PlatformDependent.hasUnsafe()) {
                        // Let us try to use sun.misc.Unsafe to replace the SelectionKeySet.
                        // This allows us to also do this in Java9+ without any extra flags.
                        long selectedKeysFieldOffset = PlatformDependent.objectFieldOffset(selectedKeysField);
                        long publicSelectedKeysFieldOffset =
                                PlatformDependent.objectFieldOffset(publicSelectedKeysField);

                        if (selectedKeysFieldOffset != -1 && publicSelectedKeysFieldOffset != -1) {
                            PlatformDependent.putObject(
                                    unwrappedSelector, selectedKeysFieldOffset, selectedKeySet);
                            PlatformDependent.putObject(
                                    unwrappedSelector, publicSelectedKeysFieldOffset, selectedKeySet);
                            return null;
                        }
                        // We could not retrieve the offset, lets try reflection as last-resort.
                    }

                    Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }
                    cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }

                    // 将unwrappedSelector中记录选择键相关的成员变量selectedKeys和publicSelectedKeys的
                    // 实例（数据结构改变）替换为优化后的selectedKeySet
                    selectedKeysField.set(unwrappedSelector, selectedKeySet);
                    publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
                    return null;
                } catch (NoSuchFieldException e) {
                    return e;
                } catch (IllegalAccessException e) {
                    return e;
                }
            }
        });

        // 如果优化操作出现异常，封装未经包装的原生selector到SelectorTuple二元组返回
        if (maybeException instanceof Exception) {
            selectedKeys = null;
            Exception e = (Exception) maybeException;
            logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, e);
            return new SelectorTuple(unwrappedSelector);
        }

        // 优化成功（即替换实例的成员成功），则保存对选择键的引用，后续将会用到
        selectedKeys = selectedKeySet;
        logger.trace("instrumented a special java.util.Set into: {}", unwrappedSelector);

        // 封装了优化了选择键的选择器二元组返回
        return new SelectorTuple(unwrappedSelector,
                                 new SelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
    }

    /**
     * Returns the {@link SelectorProvider} used by this {@link NioEventLoop} to obtain the {@link Selector}.
     */
    public SelectorProvider selectorProvider() {
        return provider;
    }

    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return newTaskQueue0(maxPendingTasks);
    }

    /**
     * 创建平台相关的任务队列
     *
     * @param maxPendingTasks 可挂起的最大任务数量
     * @return
     */
    private static Queue<Runnable> newTaskQueue0(int maxPendingTasks) {
        // This event loop never calls takeTask()
        return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.<Runnable>newMpscQueue()
                : PlatformDependent.<Runnable>newMpscQueue(maxPendingTasks);
    }

    /**
     * Registers an arbitrary {@link SelectableChannel}, not necessarily created by Netty, to the {@link Selector}
     * of this event loop.  Once the specified {@link SelectableChannel} is registered, the specified {@code task} will
     * be executed by this event loop when the {@link SelectableChannel} is ready.
     */
    public void register(final SelectableChannel ch, final int interestOps, final NioTask<?> task) {
        ObjectUtil.checkNotNull(ch, "ch");
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ch.validOps()) != 0) {
            throw new IllegalArgumentException(
                    "invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
        ObjectUtil.checkNotNull(task, "task");

        if (isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }

        if (inEventLoop()) {
            register0(ch, interestOps, task);
        } else {
            try {
                // Offload to the EventLoop as otherwise java.nio.channels.spi.AbstractSelectableChannel.register
                // may block for a long time while trying to obtain an internal lock that may be hold while selecting.
                submit(new Runnable() {
                    @Override
                    public void run() {
                        register0(ch, interestOps, task);
                    }
                }).sync();
            } catch (InterruptedException ignore) {
                // Even if interrupted we did schedule it so just mark the Thread as interrupted.
                Thread.currentThread().interrupt();
            }
        }
    }

    private void register0(SelectableChannel ch, int interestOps, NioTask<?> task) {
        try {
            ch.register(unwrappedSelector, interestOps, task);
        } catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    /**
     * Returns the percentage of the desired amount of time spent for I/O in the event loop.
     */
    public int getIoRatio() {
        return ioRatio;
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the event loop. Value range from 1-100.
     * The default value is {@code 50}, which means the event loop will try to spend the same amount of time for I/O
     * as for non-I/O tasks. The lower the number the more time can be spent on non-I/O tasks. If value set to
     * {@code 100}, this feature will be disabled and event loop will not attempt to balance I/O and non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }

    /**
     * Replaces the current {@link Selector} of this event loop with newly created {@link Selector}s to work
     * around the infamous epoll 100% CPU bug.
     */
    public void rebuildSelector() {
        if (!inEventLoop()) {
            execute(new Runnable() {
                @Override
                public void run() {
                    rebuildSelector0();
                }
            });
            return;
        }
        rebuildSelector0();
    }

    @Override
    public int registeredChannels() {
        return selector.keys().size() - cancelledKeys;
    }

    /**
     * 重建选择器
     */
    private void rebuildSelector0() {
        final Selector oldSelector = selector;
        final SelectorTuple newSelectorTuple;

        if (oldSelector == null) {
            return;
        }

        try {
            newSelectorTuple = openSelector();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        // Register all channels to the new Selector.
        int nChannels = 0;
        for (SelectionKey key: oldSelector.keys()) {
            Object a = key.attachment();
            try {
                if (!key.isValid() || key.channel().keyFor(newSelectorTuple.unwrappedSelector) != null) {
                    continue;
                }

                int interestOps = key.interestOps();
                key.cancel();
                SelectionKey newKey = key.channel().register(newSelectorTuple.unwrappedSelector, interestOps, a);
                if (a instanceof AbstractNioChannel) {
                    // Update SelectionKey
                    ((AbstractNioChannel) a).selectionKey = newKey;
                }
                nChannels ++;
            } catch (Exception e) {
                logger.warn("Failed to re-register a Channel to the new Selector.", e);
                if (a instanceof AbstractNioChannel) {
                    AbstractNioChannel ch = (AbstractNioChannel) a;
                    ch.unsafe().close(ch.unsafe().voidPromise());
                } else {
                    @SuppressWarnings("unchecked")
                    NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                    invokeChannelUnregistered(task, key, e);
                }
            }
        }

        selector = newSelectorTuple.selector;
        unwrappedSelector = newSelectorTuple.unwrappedSelector;

        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
        }
    }

    /**
     * 启动事件循环，即开始轮询，监听各种事件
     * 此方法在第一次执行任务（绑定任务或连接任务，即服务端绑定本机端口或客户端连接远程主机时调用）
     */
    @Override
    protected void run() {
        logger.info("开始轮询就绪事件及执行任务队列中的任务...");

        // 空轮询的次数
        int selectCnt = 0;

        // 开启无限循环，遍历选择器的事件，监听感兴趣的事件发生。
        // 对于服务端的主线程则是接受请求事件ACCEPT，工作线程则是可读事件READ
        // 对于客户端主线程，则是连接事件CONNECT，可读事件READ
        for (;;) {
            try {
                int strategy;
                try {
                    // 有任务会选择1次：如果tailTasks或taskQueue队列中有任务，先检测是否有就绪事件
                    // 使用非阻塞的selectNow，只会返回 >= 0
                    strategy = selectStrategy.calculateStrategy(selectNowSupplier, hasTasks());
                    logger.info("获取本轮选择策略 {}...",selectCnt,strategy);
                    switch (strategy) {
                    // strategy = -2，继续轮询，重新获取选择策略
                    case SelectStrategy.CONTINUE:
                        continue;

                    // strategy = -3 或 strategy = -1，
                    // 可能超时等待或netty认为尝试执行一次选择(select/selectNow)操作的情况
                    case SelectStrategy.BUSY_WAIT:
                        // fall-through to SELECT since the busy-wait is not supported with NIO
                    case SelectStrategy.SELECT:
                        // 等待/空轮询，从调度任务队列scheduledTaskQueue中获取下一个【调度任务】的超时时间
                        // 在调用runAllTasks()或runAllTasks(timeoutNanos)方法真正运行taskQueue队列中的任务之前，
                        // 都会先将调度任务队列scheduledTaskQueue的任务拉取到taskQueue中，再遍历执行taskQueue中的任务
                        long curDeadlineNanos = nextScheduledTaskDeadlineNanos();
                        // 如果是-1，说明没有设置超时限制，重置为最大超时时间，等同于永不超时，将一直等待
                        if (curDeadlineNanos == -1L) {
                            // NONE = Long.MAX_VALUE；
                            curDeadlineNanos = NONE; // nothing on the calendar
                        }

                        // 保存起来，后面使用
                        nextWakeupNanos.set(curDeadlineNanos);

                        logger.info("当前超时时间{},is NONE ? {}...",curDeadlineNanos,curDeadlineNanos == NONE);

                        try {
                            // 前面有任务会选择1次，这里对应没有任务的情况选择1次
                            if (!hasTasks()) {
                                // 可能是非阻塞|有限度超时阻塞|一直阻塞，其值 >=0 或出现异常
                                strategy = select(curDeadlineNanos);
                            }
                        } finally {
                            // This update is just to help block unnecessary selector wakeups
                            // so use of lazySet is ok (no race condition)
                            nextWakeupNanos.lazySet(AWAKE);
                        }
                        // fall through
                    default:
                        // strategy != (-1|-2|-3)的情况，说明执行了一次selectSupplier.get()内部调用过了selector.selectNow()
                    }
                } catch (IOException e) {
                    // 出现异常，尝试重建选择器，然后继续轮询
                    // If we receive an IOException here its because the Selector is messed up. Let's rebuild
                    // the selector and retry. https://github.com/netty/netty/issues/8566
                    rebuildSelector0();
                    selectCnt = 0;
                    handleLoopException(e);
                    continue;
                }

                // 执行到这，说明发生了一次有效的选择：
                // 不是在第1次获取选择策略时就是在第2次获取选择策略时，表示任务队列中有任务或发生了就绪事件或二者兼有之
                // 如果通道中已经有准备就绪的感兴趣的事件或队列中有任务，下面开始处理，正常是优先处理就绪事件，再处理队列中的任务

                // 有效轮询次数+1，可能时空轮询，什么也没有做
                selectCnt++;
                // 取消事件
                cancelledKeys = 0;
                // 需要重新选择
                needsToSelectAgain = false;
                // 分配处理I/O任务的时间占比，一般为50，即I/O和非I/O任务各占一半时间
                final int ioRatio = this.ioRatio;

                // 下面开始处理请求，执行任务，执行完任务之后继续回来循环遍历事件

                // 所有任务已经执行
                boolean ranTasks;

                logger.info("当前轮询{},当前等待的任务数{}...",selectCnt,pendingTasks());

                // 如果分配给I/O任务的事件占比为100%
                if (ioRatio == 100) {
                    try {
                        // 如果选择策略大于0，即代表通道中有有感兴趣的就绪事件发生
                        if (strategy > 0) {
                            // 处理准备就绪的事件，从而触发业务处理逻辑
                            processSelectedKeys();
                        }
                    } finally {
                        // 执行原来以及就绪事件产生的任务
                        // Ensure we always run tasks.
                        ranTasks = runAllTasks();
                    }
                }

                // ioRatio != 100 && strategy > 0的情况
                // 如果分配给I/O任务的事件占比不是100%，且选择策略 > 0，即通道中有感兴趣的就绪事件发生
                else if (strategy > 0) {
                    // I/O任务的开始时间戳
                    final long ioStartTime = System.nanoTime();
                    try {
                        // 处理准备就绪的事件选择键
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        // I/O任务消耗的时间
                        final long ioTime = System.nanoTime() - ioStartTime;
                        // 执行原来以及就绪事件产生的任务：即非I/O任务
                        ranTasks = runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                }

                // ioRatio != 100 && strategy <= 0的情况
                // 即通道中没有发生感兴趣的事件，尝试执行任务
                else {
                    // 直接执行任务
                    logger.info("没有就绪的I/O事件,将执行队列中的任务...");
                    ranTasks = runAllTasks(0); // This will run the minimum number of tasks
                }

                // 任务执行完毕或发生了感兴趣的事件的情况，即有任务执行或有感兴趣的事件，不是空轮询的情况
                if (ranTasks || strategy > 0) {
                    // 打印日志
                    if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS && logger.isDebugEnabled()) {
                        logger.debug("Selector.select() returned prematurely {} times in a row for Selector {}.",
                                selectCnt - 1, selector);
                    }
                    // 空轮询次数重置为0
                    selectCnt = 0;
                }

                // 不可预期的选择器唤醒：中断异常以及需要重建选择器（即解决空轮询可能导致CPU使用率100%的情况）
                else if (unexpectedSelectorWakeup(selectCnt)) { // Unexpected wakeup (unusual case)
                    // 重建了选择器，空轮询次数重置为0
                    selectCnt = 0;
                }
            } catch (CancelledKeyException e) {
                // 键被取消异常
                // Harmless exception - log anyway
                if (logger.isDebugEnabled()) {
                    logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                            selector, e);
                }
            } catch (Throwable t) {
                // 其它异常
                handleLoopException(t);
            }

            // 处理关闭事件
            // Always handle shutdown even if the loop processing threw an exception.
            try {
                // 检查是否正在关闭
                if (isShuttingDown()) {
                    closeAll();
                    if (confirmShutdown()) {
                        // 跳出循环，终止轮询
                        return;
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
        } // 结束轮询
    }

    /**
     * 是否发生了中断异常或需要重建选择器（解决因为空轮询可能导致CUP使用率100%问题）
     *
     * @param selectCnt
     * @return 返回true，说明发生了中断异常或重建选择器，应重置selectCnt为0
     */
    // returns true if selectCnt should be reset
    private boolean unexpectedSelectorWakeup(int selectCnt) {
        // 线程被中断
        if (Thread.interrupted()) {
            // Thread was interrupted so reset selected keys and break so we not run into a busy loop.
            // As this is most likely a bug in the handler of the user or it's client library we will
            // also log it.
            //
            // See https://github.com/netty/netty/issues/2426
            if (logger.isDebugEnabled()) {
                logger.debug("Selector.select() returned prematurely because " +
                        "Thread.currentThread().interrupt() was called. Use " +
                        "NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
            }
            return true;
        }

        // 重建选择器，解决可能储在空轮询导致CPU使用率100%的情况
        if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
            // The selector returned prematurely many times in a row.
            // Rebuild the selector to work around the problem.
            logger.warn("Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                    selectCnt, selector);
            rebuildSelector();
            return true;
        }
        return false;
    }

    private static void handleLoopException(Throwable t) {
        logger.warn("Unexpected exception in the selector loop.", t);

        // Prevent possible consecutive immediate failures that lead to
        // excessive CPU consumption.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }

    /**
     * 替换JavaNIO原生的SelectorKeys对象
     */
    private void processSelectedKeys() {
        logger.info("监听到就绪的I/O事件...");
        // 如果selectedKeys不为null，说明开启了选择键的优化
        if (selectedKeys != null) {
            // 使用优化的方式
            processSelectedKeysOptimized();
        } else {
            // 使用原生的key
            processSelectedKeysPlain(selector.selectedKeys());
        }
    }

    @Override
    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            logger.warn("Failed to close a selector.", e);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        cancelledKeys ++;
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            needsToSelectAgain = true;
        }
    }

    /**
     * 没有优化的选择键的处理
     *
     * @param selectedKeys
     */
    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }

        logger.info("选择键优化未开启，开始使用原始选择键遍历就绪事件...");

        // 使用无限迭代的方式处理
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();


            if(k.isWritable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_WRITE",a.getClass().getSimpleName());
            }
            if(k.isAcceptable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_ACCEPT",a.getClass().getSimpleName());
            }
            if(k.isReadable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_READ",a.getClass().getSimpleName());
            }
            if(k.isConnectable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_CONNECT",a.getClass().getSimpleName());
            }

            if (a instanceof AbstractNioChannel) {
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (!i.hasNext()) {
                break;
            }

            if (needsToSelectAgain) {
                selectAgain();
                selectedKeys = selector.selectedKeys();

                // Create the iterator again to avoid ConcurrentModificationException
                if (selectedKeys.isEmpty()) {
                    break;
                } else {
                    i = selectedKeys.iterator();
                }
            }
        }
        logger.info("结束使用原始选择键遍历就绪事件...");
    }

    /**
     * 优化后的选择键的处理
     */
    private void processSelectedKeysOptimized() {
        logger.info("开启了选择键优化，开始使用优化后选择键遍历就绪事件...");
        // 使用数组的方式遍历
        for (int i = 0; i < selectedKeys.size; ++i) {
            // 检索选择键
            final SelectionKey k = selectedKeys.keys[i];

            // 将数组中的对应键值置为null,以便在通道关闭时可以及时GC回收
            // null out entry in the array to allow to have it GC'ed once the Channel close
            // See https://github.com/netty/netty/issues/2363
            selectedKeys.keys[i] = null;

            // 获取通道中绑定的附件：再创建channel的时候放入到附加中，具体参考
            final Object a = k.attachment();

            if(k.isWritable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_WRITE",a.getClass().getSimpleName());
            }
            if(k.isAcceptable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_ACCEPT",a.getClass().getSimpleName());
            }
            if(k.isReadable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_READ",a.getClass().getSimpleName());
            }
            if(k.isConnectable()){
                logger.info("就绪的I/O事件：{},附件类型：{}...","OP_CONNECT",a.getClass().getSimpleName());
            }

            logger.info("开始处理就绪的I/O事件...");
            // 如果是AbstractNioChannel实例，说明是服务器端的子事件循环中的相关事件
            if (a instanceof AbstractNioChannel) {
                // 处理业务相关的选择键：在注册通道时，AbstractNioChannel#doRegister()方法会把自己当附件绑定到selectionKey中
                // 所有不管是NioServerSocketChannel还是NioSocketChannel，只要是netty的Nio通道都会继承AbstractNioChannel
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }
            logger.info("结束处理就绪的I/O事件...");

            if (needsToSelectAgain) {
                // null out entries in the array to allow to have it GC'ed once the Channel close
                // See https://github.com/netty/netty/issues/2363
                selectedKeys.reset(i + 1);

                selectAgain();
                i = -1;
            }
        }
        logger.info("结束使用优化后选择键遍历就绪事件...");
    }

    /**
     * 处理将交给子循环事件处理器的选择键：各种事件
     *
     * @param k
     * @param ch
     */
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        // 获取原生的I/O操作对象
        // 服务端NioServerSocketChannel返回的是AbstractNioMessageChannel.NioMessageUnsafe实例
        // 客户端以及服务端childGroup中创建的通道NioSocketChannel，返回的都是为AbstractNioByteChannel.NioByteUnsafe的实例
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();

        // 检测选择键是否有效
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                // If the channel implementation throws an exception because there is no event loop, we ignore this
                // because we are only trying to determine if ch is registered to this event loop and thus has authority
                // to close ch.
                return;
            }
            // Only close ch if ch is still registered to this EventLoop. ch could have deregistered from the event loop
            // and thus the SelectionKey could be cancelled as part of the deregistration process, but the channel is
            // still healthy and should not be closed.
            // See https://github.com/netty/netty/issues/5125
            if (eventLoop == this) {
                // close the channel if the key is not valid anymore
                unsafe.close(unsafe.voidPromise());
            }
            return;
        }

        // 开始处理选择键中响应的事件
        try {
            logger.info("正在处理就绪的I/O事件...");
            // 获取就绪的事件
            int readyOps = k.readyOps();

            // 在调用读方法read和写方法write之前，先调用finishConnect方法完成连接。否则会抛出NotYetConnectedException异常。
            // We first need to call finishConnect() before try to trigger a read(...) or write(...) as otherwise
            // the NIO JDK channel implementation may throw a NotYetConnectedException.

            // 处理连接事件
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
                // See https://github.com/netty/netty/issues/924
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);

                unsafe.finishConnect();
            }

            // Process OP_WRITE first as we may be able to write some queued buffers and so free memory.
            // 处理可写事件
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
                ch.unsafe().forceFlush();
            }

            // 处理可读或接受连接事件
            // 还要检查readOps是否为0（注册时时的初始状态），以解决可能导致自旋循环的JDK错误
            // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
            // to a spin loop
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                // 最终调用封装了I/O操作的方法读取
                // ch如果是服务端NioServerSocketChannel实例，则unsafe是AbstractNioMessageChannel.NioMessageUnsafe实例
                // ch如果是客户端以及服务端childGroup中创建的通道NioSocketChannel实例，则unsafe是AbstractNioByteChannel.NioByteUnsafe实例
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }

    /**
     * 处理Nio任务相关的选择键
     *
     * @param k
     * @param task
     */
    private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
        int state = 0;
        try {
            task.channelReady(k.channel(), k);
            state = 1;
        } catch (Exception e) {
            k.cancel();
            invokeChannelUnregistered(task, k, e);
            state = 2;
        } finally {
            switch (state) {
            case 0:
                k.cancel();
                invokeChannelUnregistered(task, k, null);
                break;
            case 1:
                if (!k.isValid()) { // Cancelled by channelReady()
                    invokeChannelUnregistered(task, k, null);
                }
                break;
            }
        }
    }

    private void closeAll() {
        selectAgain();
        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractNioChannel> channels = new ArrayList<AbstractNioChannel>(keys.size());
        for (SelectionKey k: keys) {
            Object a = k.attachment();
            if (a instanceof AbstractNioChannel) {
                channels.add((AbstractNioChannel) a);
            } else {
                k.cancel();
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                invokeChannelUnregistered(task, k, null);
            }
        }

        for (AbstractNioChannel ch: channels) {
            ch.unsafe().close(ch.unsafe().voidPromise());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k, Throwable cause) {
        try {
            task.channelUnregistered(k.channel(), cause);
        } catch (Exception e) {
            logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && nextWakeupNanos.getAndSet(AWAKE) != AWAKE) {
            selector.wakeup();
        }
    }

    @Override
    protected boolean beforeScheduledTaskSubmitted(long deadlineNanos) {
        // Note this is also correct for the nextWakeupNanos == -1 (AWAKE) case
        return deadlineNanos < nextWakeupNanos.get();
    }

    @Override
    protected boolean afterScheduledTaskSubmitted(long deadlineNanos) {
        // Note this is also correct for the nextWakeupNanos == -1 (AWAKE) case
        return deadlineNanos < nextWakeupNanos.get();
    }

    Selector unwrappedSelector() {
        return unwrappedSelector;
    }

    /**
     * 选择已经就绪的通道对应的选择键即事件数
     * 封装选择器selector的非阻塞检测方法selectNow()，不会阻塞
     *
     * @return
     * @throws IOException
     */
    int selectNow() throws IOException {
        //selector.selectNow()方法不会阻塞
        return selector.selectNow();
    }

    /**
     * 选择已经就绪的通道对应的选择键即事件数
     * 封装选择器selector的非阻塞或延时检测方法selectNow()或select(timeout)
     *
     * @param deadlineNanos
     * @return
     * @throws IOException
     */
    private int select(long deadlineNanos) throws IOException {
        // 如果是系统所能表示的最大时间（超时时间），即永不超时，调用selector.select()一直等待直到有感兴趣的事件发生
        if (deadlineNanos == NONE) {
            //selector.select()会一直阻塞
            return selector.select();
        }

        // 如果剩下的超时时间在5微妙之内，超时时间将为0：Netty认为有效的最小超时时间应该是大于5微妙以上才有意义
        // Timeout will only be 0 if deadline is within 5 microsecs
        long timeoutMillis = deadlineToDelayNanos(deadlineNanos + 995000L) / 1000000L;

        // selector.selectNow()不会阻塞
        //selector.select(timeoutMillis)和selector.select()做的事一样，不过它的阻塞有一个超时限制
        return timeoutMillis <= 0 ? selector.selectNow() : selector.select(timeoutMillis);
    }

    /**
     * 即刻再选择一次
     */
    private void selectAgain() {
        needsToSelectAgain = false;
        try {
            selector.selectNow();
        } catch (Throwable t) {
            logger.warn("Failed to update SelectionKeys.", t);
        }
    }
}
