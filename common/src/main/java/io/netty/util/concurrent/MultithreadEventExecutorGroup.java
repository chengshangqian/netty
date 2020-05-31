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
package io.netty.util.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MultithreadEventExecutorGroup是EventExecutorGroup接口实现的抽象基础类，可以同一时间里使用多线程处理任务
 *
 * handle tasks with tools 使用工具(tools)处理任务(tasks)
 *
 * Abstract base class for {@link EventExecutorGroup} implementations that handles their tasks with multiple threads at
 * the same time.
 */
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {

    /**
     * 子(线程）事件执行器数组：相当于一个线程池，EventExecutor也是EventLoop实例
     */
    private final EventExecutor[] children;

    /**
     * 子（线程）只读事件执行器集
     */
    private final Set<EventExecutor> readonlyChildren;

    /**
     * 终止(或完成调用?)的子(线程）事件执行器计数
     */
    private final AtomicInteger terminatedChildren = new AtomicInteger();

    /**
     * 所有子事件执行器终止的Promise
     */
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    /**
     * 事件执行器选择器
     */
    private final EventExecutorChooserFactory.EventExecutorChooser chooser;

    /**
     * 创建一个MultithreadEventExecutorGroup实例
     *
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance. 将被MultithreadEventExecutorGroup实例对象使用（创建）的（子）线程数量
     * @param threadFactory     the ThreadFactory to use, or {@code null} if the default should be used. 线程工厂，如果使用缺省的线程工厂，设置为null
     * @param args              arguments which will passed to each {@link #newChild(Executor, Object...)} call 将被传递给每个子事件循环（子线程）调用的参数
     */
    protected MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
    }

    /**
     * 创建一个MultithreadEventExecutorGroup实例
     *
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance. 将被MultithreadEventExecutorGroup实例对象使用（创建）的（子）线程数量
     * @param executor          the Executor to use, or {@code null} if the default should be used. 线程执行器，如果使用缺省的线程执行器，设置为null
     * @param args              arguments which will passed to each {@link #newChild(Executor, Object...)} call 将被传递给每个子事件循环（子线程）调用的参数
     */
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
    }

    /**
     * 创建一个MultithreadEventExecutorGroup实例
     *
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance. 将被MultithreadEventExecutorGroup实例对象使用（创建）的（子）线程数量
     * @param executor          the Executor to use, or {@code null} if the default should be used. （主）线程执行器，如果使用缺省的线程执行器，设置为null
     * @param chooserFactory    the {@link EventExecutorChooserFactory} to use. 事件执行器选择器工厂
     * @param args              arguments which will passed to each {@link #newChild(Executor, Object...)} call 将被传递给每个子事件循环（子线程）调用的参数
     */
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
                                            EventExecutorChooserFactory chooserFactory, Object... args) {
        // 如果指定子线程数小于等于0，抛出无效参数异常IllegalArgumentException
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        /*===> 1.创建主线程执行器 *****************************************************/
        // 如果（主）线程执行器为null，赋值一个缺省线程工厂创建的单线程任务执行器
        if (executor == null) {
            // 使用缺省线程工厂创建线程任务执行器（每个任务一个线程的线程执行器），每次执行execute方法，将由内部的线程工厂创建一个新的线程执行任务
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }

        /*===> 2.创建子（线程）事件执行器，EventExecutor也即EventLoop **********************************************/
        // 初始化子（线程）事件执行器数组，元素个数等于子线程数，每个子线程分配一个任务执行器，children数组相当于一个线程池
        children = new EventExecutor[nThreads];

        // 创建子(线程)事件执行器，填充子(线程)事件执行器数组
        for (int i = 0; i < nThreads; i ++) {
            boolean success = false;
            try {
                // 抽象模板方法创建子(线程)事件执行器,见{@NioEventLoopGroup#newChild(...)}
                // children为EventExecutor类型数组，NioEventLoopGroup#newChild(...)方法返回的是NioEventLoop，NioEventLoop通过继承/实现路径，实际上是EventExecutor的实现类
                // NioEventLoop > SingleThreadEventLoop > SingleThreadEventExecutor >
                // (AbstractScheduledEventExecutor > AbstractEventExecutor ) | (OrderedEventExecutor) > EventExecutor
                // 创建nThreads个EventLoop即EventExecutor
                children[i] = newChild(executor, args);
                success = true;
            } catch (Exception e) {
                // TODO: Think about if this is a good exception type
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                // 如果本次子事件处理器创建不成功，将优雅关闭本次之前创建的子事件执行器
                if (!success) {
                    for (int j = 0; j < i; j ++) {
                        children[j].shutdownGracefully();
                    }

                    // 检查所有的事件执行器是否已经终止，如果还没有终止，一直等待其终止或超时（超时事件为Integer.MAX_VALUE秒，相当于无限期等待?）
                    for (int j = 0; j < i; j ++) {
                        EventExecutor e = children[j];
                        // 通过上面分析创建的children的继承链，
                        // e为SingleThreadEventExecutor实例，以下方法将条用SingleThreadEventExecutor实例的方法
                        try {
                            // 等待事件执行器线程终止直到超时
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            // 中断当前线程
                            // Let the caller handle the interruption.
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        /*===> 3.初始化子（线程）事件执行器的选择器 ******************************************/
        // 使用事件执行器选择器工厂为子事件执行器创建选择器：后续由一个客户端请求进来时，将选择其中一个子事件执行器创建线程以及绑定/注册对应的Channel和Selector等
        chooser = chooserFactory.newChooser(children);

        /*===> 4.创建和绑定子（线程）事件执行器的终止/完成执行任务的监听器 ***************************/
        // 创建终止监听器
        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            /**
             * 终止成功，触发次方法：即监控操作完成事件
             *
             * @param future  the source {@link Future} which called this callback 调用次回调方法的源对象或未来发生中断事件的源对象{@link Future}
             * @throws Exception
             */
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                // 如果中断的子事件执行器（子线程）数量与子事件执行器数量相等
                if (terminatedChildren.incrementAndGet() == children.length) {
                    // 即所有子事件执行器终止，触发terminationFuture的success：null表示成功
                    terminationFuture.setSuccess(null);
                }
            }
        };

        // 为子事件执行器添加未来事件（终止事件，表示调用结束或完成？）监听器
        for (EventExecutor e: children) {
            e.terminationFuture().addListener(terminationListener);
        }

        /*===> 5.初始化只读子事件执行器集合 ***************************/
        Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);
        Collections.addAll(childrenSet, children);
        readonlyChildren = Collections.unmodifiableSet(childrenSet);
    }

    /**
     * 创建一个缺省的线程工厂
     *
     * @return 缺省的线程工厂对象
     */
    protected ThreadFactory newDefaultThreadFactory() {
        // 为当前类型创建一个缺省的线程工厂，内部线程组或线程池中可持有最多5个线程
        return new DefaultThreadFactory(getClass());
    }

    /**
     * 选择下一个子事件执行器
     *
     * @return
     */
    @Override
    public EventExecutor next() {
        // 遍历选择下一个
        return chooser.next();
    }

    /**
     * 迭代只读子事件执行器
     *
     * @return
     */
    @Override
    public Iterator<EventExecutor> iterator() {
        return readonlyChildren.iterator();
    }

    /**
     * 返回本实例的子事件执行器{@link EventExecutor}数量。其数量和子线程数量是1:1关系，即一个子线程对应一个子事件处理器。
     * Return the number of {@link EventExecutor} this implementation uses. This number is the maps
     * 1:1 to the threads it use.
     */
    public final int executorCount() {
        return children.length;
    }

    /**
     * 抽象模板方法，由具体子类取实现，创建一个新的事件执行器实例，此实例稍后通过调用{@link #next()}方法后访问它。
     * newChild方法将被服务于{@link MultithreadEventExecutorGroup}的每个线程调用。
     *
     * via 通过，经由
     *
     * Create a new EventExecutor which will later then accessible via the {@link #next()}  method. This method will be
     * called for each thread that will serve this {@link MultithreadEventExecutorGroup}.
     *
     */
    protected abstract EventExecutor newChild(Executor executor, Object... args) throws Exception;

    /**
     * 优雅关闭（所有）子事件执行器
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout     the maximum amount of time to wait until the executor is {@linkplain #shutdown()}
     *                    regardless if a task was submitted during the quiet period
     * @param unit        the unit of {@code quietPeriod} and {@code timeout}
     *
     * @return
     */
    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        for (EventExecutor l: children) {
            l.shutdownGracefully(quietPeriod, timeout, unit);
        }

        // 返回当前终止的未来调用
        return terminationFuture();
    }

    /**
     * 返回当前终止的未来调用
     *
     * @return
     */
    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    /**
     * 关闭（所有）子事件执行器，已弃用
     * 建议使用{@link MultithreadEventExecutorGroup#shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()}
     */
    @Override
    @Deprecated
    public void shutdown() {
        for (EventExecutor l: children) {
            l.shutdown();
        }
    }

    /**
     * （所有）子事件执行器是否正在关闭
     *
     * @return
     */
    @Override
    public boolean isShuttingDown() {
        for (EventExecutor l: children) {
            if (!l.isShuttingDown()) {
                return false;
            }
        }
        return true;
    }

    /**
     * （所有）子事件执行器是否已关闭
     *
     * @return
     */
    @Override
    public boolean isShutdown() {
        for (EventExecutor l: children) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    /**
     * （所有）子事件执行器是否已经终止
     *
     * @return
     */
    @Override
    public boolean isTerminated() {
        for (EventExecutor l: children) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 等待（所有）子事件执行器终止
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        // 死线，即等待的超时时间界限
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        // 对于每一个子事件执行器，一直等待其成功终止或超时
        loop: for (EventExecutor l: children) {
            // 对于当前的子事件执行器l，等待成功终止或超时
            for (;;) {
                // 剩余时间
                long timeLeft = deadline - System.nanoTime();

                // 如果总的等待时间已经超时，直接跳出（非继续continue）外层循环
                if (timeLeft <= 0) {
                    break loop;
                }

                // 递归?：等待当前子事件执行器l终止，成功则跳出本层循环继续外层循环，否则等待超时直到跳出外层循环
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }

        // 返回所有子事件执行器终止状态
        return isTerminated();
    }
}
