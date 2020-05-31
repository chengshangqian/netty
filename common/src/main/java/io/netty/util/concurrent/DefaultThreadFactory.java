/*
 * Copyright 2013 The Netty Project
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

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一个使用简单命名规则的现场工厂{@link ThreadFactory}实现类
 *
 * A {@link ThreadFactory} implementation with a simple naming rule.
 */
public class DefaultThreadFactory implements ThreadFactory {

    /**
     * 线程池原子id：线程池唯一标识
     */
    private static final AtomicInteger poolId = new AtomicInteger();

    /**
     * 下一个线程原子id：由当前线程工厂创建的线程的唯一标识
     */
    private final AtomicInteger nextId = new AtomicInteger();

    /**
     * 当前线程工厂创建的线程的名字前缀
     */
    private final String prefix;

    /**
     * 是否有守护进程
     */
    private final boolean daemon;

    /**
     * 线程优先级
     */
    private final int priority;

    /**
     * 线程组：持有线程的组，即线程池
     */
    protected final ThreadGroup threadGroup;

    /**
     * 指定线程池类型、没有守护进程daemon、线程为普通优先级
     * 创建一个缺省的线程工厂
     *
     * @param poolType 线程池类型，用作线程池名字
     */
    public DefaultThreadFactory(Class<?> poolType) {
        this(poolType, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName) {
        this(poolName, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon) {
        this(poolType, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName, boolean daemon) {
        this(poolName, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, int priority) {
        this(poolType, false, priority);
    }

    public DefaultThreadFactory(String poolName, int priority) {
        this(poolName, false, priority);
    }

    /**
     * 指定线程池类型、是否创建守护线程、线程优先级
     * 创建一个DefaultThreadFactory实例
     *
     * @param poolType 线程池类型
     * @param daemon 是否有守护进程
     * @param priority 优先级
     */
    public DefaultThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        this(toPoolName(poolType), daemon, priority);
    }

    /**
     * 将线程池类型转换为线程池名
     *
     * @param poolType
     * @return
     */
    public static String toPoolName(Class<?> poolType) {
        // 空指针检查
        ObjectUtil.checkNotNull(poolType, "poolType");

        // 获取简化的线程池名字
        String poolName = StringUtil.simpleClassName(poolType);
        switch (poolName.length()) {
            case 0:
                // 空
                return "unknown";
            case 1:
                // 只有一个字符，转为小写（规范的类名如果只有一个字符，正常应该为大写）
                return poolName.toLowerCase(Locale.US);
            default:
                // 大于一个字符,如果首字母大写，第2个字母小写，则将首字母小写
                if (Character.isUpperCase(poolName.charAt(0)) && Character.isLowerCase(poolName.charAt(1))) {
                    return Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1);
                } else {
                    // 其它情况原样返回：前两个字母大写也原样返回
                    return poolName;
                }
        }
    }

    /**
     * 指定线程池名、是否创建守护线程、线程优先级、线程组及线程池
     * 创建一个缺省线程工厂DefaultThreadFactory实例
     *
     * @param poolName 线程池名
     * @param daemon 是否创建守护线程
     * @param priority 线程优先级，范围1-10
     * @param threadGroup 线程组及线程池
     */
    public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        // 线程池名字空指针检查
        ObjectUtil.checkNotNull(poolName, "poolName");

        /**
         * 检查优先级priority是否在1-10之间（Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY），不符合将抛出无效参数异常IllegalArgumentException
         */
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }

        // 线程名字前缀：线程池名-线程池id自增（相当于线程池唯一id）-
        prefix = poolName + '-' + poolId.incrementAndGet() + '-';

        // 初始化其它成员变量
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
    }

    /**
     * 指定线程池名、是否创建守护线程、线程优先级
     * 创建一个缺省线程工厂DefaultThreadFactory实例
     *
     * @param poolName 线程池名
     * @param daemon 是否创建守护线程
     * @param priority 线程优先级，范围1-10，Thread提供了一些静态常量，如最小优先级Thread.MIN_PRIORITY 1，缺省的默认优先级Thread.NORM_PRIORITY 5，最大优先级Thread.MAX_PRIORITY 10
     */
    public DefaultThreadFactory(String poolName, boolean daemon, int priority) {
        this(poolName, daemon, priority, System.getSecurityManager() == null ?
                Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup());
    }

    /**
     * 为任务分配一个线程：运行时调用
     *
     * @param r 任务
     * @return 返回线程实例
     */
    @Override
    public Thread newThread(Runnable r) {
        /**
         * 为原始任务分配/创建一个FastThreadLocal类型的线程，线程名使用【线程池前缀+下一个线程id】命名
         */
        Thread t = newThread(FastThreadLocalRunnable.wrap(r), prefix + nextId.incrementAndGet());

        try {
            // 设置守护进程
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            // 设置线程优先级
            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) {
            //设置线程的守护进程或优先级失败，可以忽略
            // Doesn't matter even if failed to set.
        }

        // 返回线程实例
        return t;
    }

    /**
     * 为任务分配一个线程，并绑定一个线程名称：运行时调用
     *
     * @param r 任务
     * @param name 线程名称
     * @return 执行任务的线程
     */
    protected Thread newThread(Runnable r, String name) {
        // 创建一个FastThreadLocal类型的线程
        return new FastThreadLocalThread(threadGroup, r, name);
    }
}
