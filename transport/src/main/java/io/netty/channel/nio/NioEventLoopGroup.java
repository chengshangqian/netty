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
import io.netty.channel.EventLoop;
import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SelectStrategyFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;

import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * NioEventLoopGroup，非阻塞事件循环组
 * 它是多线程事件循环组{@link MultithreadEventLoopGroup}的实现，其超类同时实现了NIO的ExecutorService接口,即本身就是线程池。
 * 它用于基于NIO选择器Selector的通道上。
 *
 * used for 用于
 * A based B B 基于 A
 * A based on B A 基于 B
 *
 * {@link MultithreadEventLoopGroup} implementations which is used for NIO {@link Selector} based {@link Channel}s.
 */
public class NioEventLoopGroup extends MultithreadEventLoopGroup {

    /**
     * 使用缺省线程数、缺省的线程工厂 {@link ThreadFactory}以及调用静态方法 {@link SelectorProvider#provider()}返回的选择器提供器{@link SelectorProvider}来
     * 创建一个新的NioEventLoopGroup实例
     *
     * Create a new instance using the default number of threads, the default {@link ThreadFactory} and
     * the {@link SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     */
    public NioEventLoopGroup() {
        // nThreads为0表示使用默认线程数：主机CPU逻辑核心数 * 2，见MultithreadEventLoopGroup的DEFAULT_EVENT_LOOP_THREADS属性
        this(0);
    }

    /**
     * 使用指定初始线程数、缺省的线程工厂 {@link ThreadFactory}以及调用静态方法 {@link SelectorProvider#provider()}返回的选择器提供器{@link SelectorProvider}来
     * 新建一个NioEventLoopGroup实例
     *
     * specified 明确规定、指定
     * the specified number of threads 明确指定的线程数量
     *
     * Create a new instance using the specified number of threads, {@link ThreadFactory} and the
     * {@link SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     *
     * @param nThreads 初始线程数
     */
    public NioEventLoopGroup(int nThreads) {
        // 空线程执行器
        this(nThreads, (Executor) null);
    }

    /**
     * 使用缺省线程数、指定的线程工厂 {@link ThreadFactory}以及调用静态方法 {@link SelectorProvider#provider()}返回的选择器提供器{@link SelectorProvider}来
     * 新建一个NioEventLoopGroup实例
     *
     * given 给定的
     *
     * Create a new instance using the default number of threads, the given {@link ThreadFactory} and the
     * {@link SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     *
     * @param threadFactory 线程工厂
     */
    public NioEventLoopGroup(ThreadFactory threadFactory) {
        this(0, threadFactory, SelectorProvider.provider());
    }

    /**
     * 使用指定的初始线程数、指定的线程工厂{@link ThreadFactory}以及调用静态方法 {@link SelectorProvider#provider()}返回的选择器提供器{@link SelectorProvider}来
     * 新建一个NioEventLoopGroup实例
     *
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory} and the
     * {@link SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     *
     * @param nThreads 初始线程数
     * @param threadFactory 线程工厂
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SelectorProvider.provider());
    }

    /**
     * 使用指定的初始线程数、线程执行器{@link Executor}以及调用静态方法 {@link SelectorProvider#provider()}返回的选择器提供器{@link SelectorProvider}来
     * 创建一个NioEventLoopGroup实例
     *
     * @param nThreads 初始线程数
     * @param executor 线程执行器
     */
    public NioEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }

    /**
     * 使用指定的初始线程数、指定的线程工厂{@link ThreadFactory}以及指定的选择器提供器{@link SelectorProvider}、
     * 缺省的选择策略工厂{@link DefaultSelectStrategyFactory#INSTANCE}来
     * 创建一个NioEventLoopGroup实例
     *
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory} and the given
     * {@link SelectorProvider}.
     *
     * @param nThreads 初始线程数
     * @param threadFactory 线程工厂
     * @param selectorProvider 选择器提供者
     */
    public NioEventLoopGroup(
            int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider) {
        this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    /**
     * 使用指定的初始线程数、指定的线程工厂{@link ThreadFactory}以及指定的选择器提供器{@link SelectorProvider}、指定的选择策略工厂来{@link SelectStrategyFactory}来
     * 创建一个NioEventLoopGroup实例
     *
     * @param nThreads 初始线程数
     * @param threadFactory 线程工厂
     * @param selectorProvider 选择器提供者
     * @param selectStrategyFactory 选择策略工厂
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory,
        final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
        // 调用父类构造函数
        super(nThreads, threadFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    /**
     * 使用指定的初始线程数、线程执行器{@link Executor}以及指定的选择器提供器{@link SelectorProvider}、
     * 缺省的选择策略工厂{@link DefaultSelectStrategyFactory#INSTANCE}来
     * 创建一个NioEventLoopGroup实例
     *
     * @param nThreads 初始线程数
     * @param executor 线程执行器
     * @param selectorProvider 选择器提供者
     */
    public NioEventLoopGroup(
            int nThreads, Executor executor, final SelectorProvider selectorProvider) {
        this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    /**
     * 使用指定的初始线程数、线程执行器{@link Executor}以及指定的选择器提供器{@link SelectorProvider}、选择策略工厂{@link SelectStrategyFactory}、
     * 以及RejectedExecutionHandlers.reject()返回的拒绝执行处理器{@link RejectedExecutionHandler}来
     * 创建一个NioEventLoopGroup实例
     *
     * @param nThreads 初始线程数
     * @param executor 线程执行器
     * @param selectorProvider 选择器提供者
     * @param selectStrategyFactory 选择策略工厂
     */
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory) {
        // 调用父类构造方法 => 0,null,default,default,default
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    /**
     * 使用指定的初始线程数、线程执行器{@link Executor}、事件执行器选择器工厂{@link EventExecutorChooserFactory}、 选择器提供器{@link SelectorProvider}、
     * 选择策略工厂{@link SelectStrategyFactory}以及RejectedExecutionHandlers.reject()返回的拒绝执行处理器{@link RejectedExecutionHandler}来
     * 创建一个NioEventLoopGroup实例
     *
     * @param nThreads 初始线程数
     * @param executor 线程执行器
     * @param chooserFactory 事件执行器选择器工厂
     * @param selectorProvider 选择器提供者
     * @param selectStrategyFactory 选择策略工厂
     */
    public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
                             final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory) {
        // 调用父类构造方法
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory,
                RejectedExecutionHandlers.reject());
    }

    /**
     * 使用指定的初始线程数、线程执行器{@link Executor}、事件执行器选择器工厂{@link EventExecutorChooserFactory}、 选择器提供器{@link SelectorProvider}、
     * 选择策略工厂{@link SelectStrategyFactory}、拒绝执行处理器{@link RejectedExecutionHandler}来
     * 创建一个NioEventLoopGroup实例
     *
     * @param nThreads 初始线程数
     * @param executor 线程执行器
     * @param chooserFactory 事件执行器选择器工厂
     * @param selectorProvider 选择器提供者
     * @param selectStrategyFactory 选择策略工厂
     * @param rejectedExecutionHandler 拒绝执行处理器
     */
    public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
                             final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory,
                             final RejectedExecutionHandler rejectedExecutionHandler) {
        // 调用父类构造方法
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler);
    }

    /**
     * 使用指定的初始线程数、线程执行器{@link Executor}、事件执行器选择器工厂{@link EventExecutorChooserFactory}、 选择器提供器{@link SelectorProvider}、
     * 选择策略工厂{@link SelectStrategyFactory}、拒绝执行处理器{@link RejectedExecutionHandler}、任务队列工厂{@link EventLoopTaskQueueFactory}来
     * 创建一个NioEventLoopGroup实例
     *
     * @param nThreads 初始线程数
     * @param executor 线程执行器
     * @param chooserFactory 事件执行器选择器工厂
     * @param selectorProvider 选择器提供者
     * @param selectStrategyFactory 选择策略工厂
     * @param rejectedExecutionHandler 拒绝执行处理器
     * @param taskQueueFactory 事件循环任务队列工厂
     */
    public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
                             final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory,
                             final RejectedExecutionHandler rejectedExecutionHandler,
                             final EventLoopTaskQueueFactory taskQueueFactory) {
        // 调用父类构造方法
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory,
                rejectedExecutionHandler, taskQueueFactory);
    }

    /**
     * 设置子事件循环（子线程）中I/O任务所需要花费的时间占比。缺省值时50（即50%），即事件循环将尝试为I/O任务花费与非I/O任务相同的处理时间
     *
     * the percentage of sth 某事的百分比
     * the desired amount of time 渴望/需要的时间数量
     * spent for sth 为某事消耗/花费（的时间/金钱）
     * as 如同
     * to spend the same amount of time for I/O as for non-I/O tasks 为I/O任务消耗/花费如同非I/O任务一样的时间数量
     *
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.  The default value is
     * {@code 50}, which means the event loop will try to spend the same amount of time for I/O as for non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        for (EventExecutor e: this) {
            ((NioEventLoop) e).setIoRatio(ioRatio);
        }
    }

    /**
     * 使用新创建的选择器{@link Selector}替换当前子事件循环（子线程）的选择器{@link Selector}，以解决臭名昭著的【epoll占用CPU使用100%】的问题.
     *
     * replace A with B 使用B替换/代替A
     * to work around sth 绕过某事情以（继续）工作
     *
     * Replaces the current {@link Selector}s of the child event loops with newly created {@link Selector}s to work
     * around the  infamous epoll 100% CPU bug.
     */
    public void rebuildSelectors() {
        for (EventExecutor e: this) {
            ((NioEventLoop) e).rebuildSelector();
        }
    }

    /**
     * 创建子事件循环，内部主要封装了执行器executor、以及选择器selector等
     *
     * @param executor （主）线程执行器
     * @param args 参数
     * @return 事件循环实例
     * @throws Exception
     */
    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        // 事件循环任务队列工厂
        EventLoopTaskQueueFactory queueFactory = args.length == 4 ? (EventLoopTaskQueueFactory) args[3] : null;

        // 返回NioEventLoop实例，其超类接口本身也继承或实现了Java NIO的ExecutorService接口，所以NioEventLoop也是一个线程池
        return new NioEventLoop(this, executor, (SelectorProvider) args[0],
            ((SelectStrategyFactory) args[1]).newSelectStrategy(), (RejectedExecutionHandler) args[2], queueFactory);
    }
}
