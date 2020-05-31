/*
 * Copyright 2014 The Netty Project
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

package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 地址解析器组
 * 创建和管理地址解析器{@link NameResolver}，让每个事件执行器{@link EventExecutor}可以拥有自己的地址解析器实例
 *
 * Creates and manages {@link NameResolver}s so that each {@link EventExecutor} has its own resolver instance.
 */
public abstract class AddressResolverGroup<T extends SocketAddress> implements Closeable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AddressResolverGroup.class);

    /**
     * Note that we do not use a {@link ConcurrentMap} here because it is usually expensive to instantiate a resolver.
     */
    private final Map<EventExecutor, AddressResolver<T>> resolvers =
            new IdentityHashMap<EventExecutor, AddressResolver<T>>();

    private final Map<EventExecutor, GenericFutureListener<Future<Object>>> executorTerminationListeners =
            new IdentityHashMap<EventExecutor, GenericFutureListener<Future<Object>>>();

    protected AddressResolverGroup() { }

    /**
     * 返回关联指定事件执行器{@link EventExecutor}的地址解析器{@link AddressResolver}。
     * 如果指定的事件执行器还没有地址解析器，这个方法将会调用{@link #newResolver(EventExecutor)}方法创建一个地址解析器并返回。
     * 调用{@link #newResolver(EventExecutor)}方法创建的地址解析器，
     * 可以通过调用另外一个指定相同的{@link EventExecutor}参数的方法{@code #getResolver(EventExecutor)}而实现复用。
     *
     * Returns the {@link AddressResolver} associated with the specified {@link EventExecutor}. If there's no associated
     * resolver found, this method creates and returns a new resolver instance created by
     * {@link #newResolver(EventExecutor)} so that the new resolver is reused on another
     * {@code #getResolver(EventExecutor)} call with the same {@link EventExecutor}.
     */
    public AddressResolver<T> getResolver(final EventExecutor executor) {
        // 事件执行器不能为nul：EventLoop实例也是EventExecutor的实例
        ObjectUtil.checkNotNull(executor, "executor");

        // 如果事件执行器正在关闭，将抛出异常
        if (executor.isShuttingDown()) {
            throw new IllegalStateException("executor not accepting a task");
        }

        // 地址解析器
        AddressResolver<T> r;

        // 同步锁，
        synchronized (resolvers) {
            // 检查地址解析器组缓存中是否存在该执行器相关的地址解析器
            r = resolvers.get(executor);

            // 如果不存在，将新建一个地址解析器
            if (r == null) {
                // 新的地址解析器实例
                final AddressResolver<T> newResolver;
                try {
                    newResolver = newResolver(executor);
                } catch (Exception e) {
                    throw new IllegalStateException("failed to create a new resolver", e);
                }

                // 将新的地址解析器放入到地址解析器组当中
                resolvers.put(executor, newResolver);

                /**
                 * 注册未来事件，监听地址解析器的销毁
                 */
                final FutureListener<Object> terminationListener = new FutureListener<Object>() {
                    @Override
                    public void operationComplete(Future<Object> future) {
                        synchronized (resolvers) {
                            resolvers.remove(executor);
                            executorTerminationListeners.remove(executor);
                        }
                        newResolver.close();
                    }
                };

                // 绑定监听器
                executorTerminationListeners.put(executor, terminationListener);
                executor.terminationFuture().addListener(terminationListener);

                // 赋值给地址解析器
                r = newResolver;
            }
        }

        // 返回地址解析器
        return r;
    }

    /**
     * Invoked by {@link #getResolver(EventExecutor)} to create a new {@link AddressResolver}.
     */
    protected abstract AddressResolver<T> newResolver(EventExecutor executor) throws Exception;

    /**
     * Closes all {@link NameResolver}s created by this group.
     */
    @Override
    @SuppressWarnings({ "unchecked", "SuspiciousToArrayCall" })
    public void close() {
        final AddressResolver<T>[] rArray;
        final Map.Entry<EventExecutor, GenericFutureListener<Future<Object>>>[] listeners;

        synchronized (resolvers) {
            rArray = (AddressResolver<T>[]) resolvers.values().toArray(new AddressResolver[0]);
            resolvers.clear();
            listeners = executorTerminationListeners.entrySet().toArray(new Map.Entry[0]);
            executorTerminationListeners.clear();
        }

        for (final Map.Entry<EventExecutor, GenericFutureListener<Future<Object>>> entry : listeners) {
            entry.getKey().terminationFuture().removeListener(entry.getValue());
        }

        for (final AddressResolver<T> r: rArray) {
            try {
                r.close();
            } catch (Throwable t) {
                logger.warn("Failed to close a resolver:", t);
            }
        }
    }
}
