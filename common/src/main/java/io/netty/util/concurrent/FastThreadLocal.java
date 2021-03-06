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
package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.PlatformDependent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * {@link FastThreadLocal}是一个专门设计的{@link ThreadLocal}（线程上下文、线程内部变量、线程局部变量,用于存储线程相关的变量或信息）变体类/派生类（功能类似）
 * 相比Thread内访问ThreadLocal中的内容，使用快速线程本地变量线程类{@link FastThreadLocalThread}访问{@link FastThreadLocal}类可以提供更高访问性能
 *
 * variant 变体、派生
 * yields 提供
 * performance 性能，表现
 *
 * A special variant of {@link ThreadLocal} that yields higher access performance when accessed from a
 * {@link FastThreadLocalThread}.
 *
 * 在{@link FastThreadLocal}内部，{@link FastThreadLocal}实例使用一个数组的常量索引，代替使用哈希码或哈希表，来查找一个变量。
 * 虽然看起来很微妙，但它提供了轻微的性能优势相较于使用哈希表，并且它在频繁访问线程内部变量存储的内容时非常有用。
 *
 * subtle 微妙的
 * slight 轻微的，细小的
 * frequently 频繁地
 *
 * <p>
 * Internally, a {@link FastThreadLocal} uses a constant index in an array, instead of using hash code and hash table,
 * to look for a variable.  Although seemingly very subtle, it yields slight performance advantage over using a hash
 * table, and it is useful when accessed frequently.
 * </p>
 *
 * 要获取这个线程内部变量（线程上下文、线程局部变量）即@link FastThreadLocalThread}的优势，你的线程必须时一个FastThreadLocal类型支持的线程即{@link FastThreadLocalThread}线程或它的子类型。
 * 一般情况下，由于这个原因，缺省线程工厂{@link DefaultThreadFactory}创建的所有的线程都是{@link FastThreadLocalThread}线程。
 *
 * variable 变量
 * due to 由于
 *
 * <p>
 * To take advantage of this thread-local variable, your thread must be a {@link FastThreadLocalThread} or its subtype.
 * By default, all threads created by {@link DefaultThreadFactory} are {@link FastThreadLocalThread} due to this reason.
 * </p>
 *
 * 在线程上，最快的实现方式就是扩展继承{@link FastThreadLocalThread}，因为它要求一个专门的成员存储需要的状态信息。
 * 任何其它类型的线程使用{@link FastThreadLocal}，访问返回的则是一个普通的{@link ThreadLocal}。
 *
 * regular 常规的
 *
 * <p>
 * Note that the fast path is only possible on threads that extend {@link FastThreadLocalThread}, because it requires
 * a special field to store the necessary state.  An access by any other kind of thread falls back to a regular
 * {@link ThreadLocal}.
 * </p>
 *
 * @param <V> the type of the thread-local variable
 * @see ThreadLocal
 */
public class FastThreadLocal<V> {

    /**
     * 将要删除的变量索引：用完后将要被删除的线程内部变量存储在线程FastThreadLocal内部InternalThreadLocalMap中对应的数组的索引即位置
     */
    private static final int variablesToRemoveIndex = InternalThreadLocalMap.nextVariableIndex();

    /**
     * 移除所有绑定到当前线程的{@link FastThreadLocal}变量。这个操作非常有用，当你在一个容器环境中，且不想留下/保留你没有管理的线程内的线程局部变量时。
     *
     * Removes all {@link FastThreadLocal} variables bound to the current thread.  This operation is useful when you
     * are in a container environment, and you don't want to leave the thread local variables in the threads you do not
     * manage.
     */
    public static void removeAll() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap == null) {
            return;
        }

        try {
            Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
            if (v != null && v != InternalThreadLocalMap.UNSET) {
                @SuppressWarnings("unchecked")
                Set<FastThreadLocal<?>> variablesToRemove = (Set<FastThreadLocal<?>>) v;
                FastThreadLocal<?>[] variablesToRemoveArray =
                        variablesToRemove.toArray(new FastThreadLocal[0]);
                for (FastThreadLocal<?> tlv: variablesToRemoveArray) {
                    tlv.remove(threadLocalMap);
                }
            }
        } finally {
            InternalThreadLocalMap.remove();
        }
    }

    /**
     * Returns the number of thread local variables bound to the current thread.
     */
    public static int size() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap == null) {
            return 0;
        } else {
            return threadLocalMap.size();
        }
    }

    /**
     * Destroys the data structure that keeps all {@link FastThreadLocal} variables accessed from
     * non-{@link FastThreadLocalThread}s.  This operation is useful when you are in a container environment, and you
     * do not want to leave the thread local variables in the threads you do not manage.  Call this method when your
     * application is being unloaded from the container.
     */
    public static void destroy() {
        InternalThreadLocalMap.destroy();
    }

    @SuppressWarnings("unchecked")
    private static void addToVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable) {
        Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
        Set<FastThreadLocal<?>> variablesToRemove;
        if (v == InternalThreadLocalMap.UNSET || v == null) {
            variablesToRemove = Collections.newSetFromMap(new IdentityHashMap<FastThreadLocal<?>, Boolean>());
            threadLocalMap.setIndexedVariable(variablesToRemoveIndex, variablesToRemove);
        } else {
            variablesToRemove = (Set<FastThreadLocal<?>>) v;
        }

        variablesToRemove.add(variable);
    }

    private static void removeFromVariablesToRemove(
            InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable) {

        Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);

        if (v == InternalThreadLocalMap.UNSET || v == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Set<FastThreadLocal<?>> variablesToRemove = (Set<FastThreadLocal<?>>) v;
        variablesToRemove.remove(variable);
    }

    /**
     * 索引，不可变
     */
    private final int index;

    /**
     * 创建一个快速线程内部FastThreadLocal实例
     */
    public FastThreadLocal() {
        // 原子索引，整个应用程序中不会重复，类似数据库中的自增序列
        // 当index大于InternalThreadLocalMap内部父类中Object[]数组indexedVariables初始化的长度时，
        // 在调用存储对象的set方法时，会自动扩容后再存储。
        // 每创建一个FastThreadLocal对象，会获得一个index索引，即每个FastThreadLocal实例持有一个不可变的index，
        // 不同实例的FastThreadLocal对象，其index不相同，但用于存取变量用的InternalThreadLocalMap实例【相同线程下】只有一个。
        // 这意味着如果需要存取多个不同的线程内的共享对象，创建多个FastThreadLocal实例来存取即可
        index = InternalThreadLocalMap.nextVariableIndex();
    }

    /**
     * 返回当前线程下存储的共享变量的值，将调用initialize方法，
     * 如果initialize方法被子类重载，则可以新建对象实例，当然也可以手动调用set设置
     *
     * Returns the current value for the current thread
     */
    @SuppressWarnings("unchecked")
    public final V get() {
        // 获取存储归属【当前线程】共享对象的threadLocalMap(即每个线程会有自己的threadLocalMap)
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        // 在当前线程下的threadLocalMap中，
        // 从threadLocalMap中的indexedVariables数组中检索位于index位置的对象
        Object v = threadLocalMap.indexedVariable(index);

        if (v != InternalThreadLocalMap.UNSET) {
            return (V) v;
        }

        // 如果线程内部找不到，初始化
        return initialize(threadLocalMap);
    }

    /**
     * Returns the current value for the current thread if it exists, {@code null} otherwise.
     */
    @SuppressWarnings("unchecked")
    public final V getIfExists() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap != null) {
            Object v = threadLocalMap.indexedVariable(index);
            if (v != InternalThreadLocalMap.UNSET) {
                return (V) v;
            }
        }
        return null;
    }

    /**
     * 返回当前线程下存储的共享变量的值
     *
     * Returns the current value for the specified thread local map.
     * The specified thread local map must be for the current thread.
     */
    @SuppressWarnings("unchecked")
    public final V get(InternalThreadLocalMap threadLocalMap) {
        // 获取每个FastThreadLocal对象绑定的线程内部共享对象
        Object v = threadLocalMap.indexedVariable(index);

        // 如果获取到的对象不是未设置对象，则返回
        if (v != InternalThreadLocalMap.UNSET) {
            return (V) v;
        }

        // 否则将调用initialize方法创建
        return initialize(threadLocalMap);
    }

    /**
     * 初始化实例
     *
     * @param threadLocalMap
     * @return
     */
    private V initialize(InternalThreadLocalMap threadLocalMap) {
        V v = null;
        try {
            // 调用initialValue创建实例，该方法可以被子类重载，
            // 如果没有重载，返回null
            v = initialValue();
        } catch (Exception e) {
            PlatformDependent.throwException(e);
        }

        // 创建后，存放到当前的threadLocalMap中对应的index位置
        threadLocalMap.setIndexedVariable(index, v);

        addToVariablesToRemove(threadLocalMap, this);
        return v;
    }

    /**
     * Set the value for the current thread.
     */
    public final void set(V value) {
        if (value != InternalThreadLocalMap.UNSET) {
            InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
            setKnownNotUnset(threadLocalMap, value);
        } else {
            // 从数组中移除：重置为UNSET对象
            remove();
        }
    }

    /**
     * 有没有可能通过闭包或异步回调的方式从当前线程操作其它线程的内部变量?
     *
     * Set the value for the specified thread local map. The specified thread local map must be for the current thread.
     */
    public final void set(InternalThreadLocalMap threadLocalMap, V value) {
        if (value != InternalThreadLocalMap.UNSET) {
            setKnownNotUnset(threadLocalMap, value);
        } else {
            remove(threadLocalMap);
        }
    }

    /**
     * @return see {@link InternalThreadLocalMap#setIndexedVariable(int, Object)}.
     */
    private void setKnownNotUnset(InternalThreadLocalMap threadLocalMap, V value) {
        if (threadLocalMap.setIndexedVariable(index, value)) {
            addToVariablesToRemove(threadLocalMap, this);
        }
    }

    /**
     * Returns {@code true} if and only if this thread-local variable is set.
     */
    public final boolean isSet() {
        return isSet(InternalThreadLocalMap.getIfSet());
    }

    /**
     * Returns {@code true} if and only if this thread-local variable is set.
     * The specified thread local map must be for the current thread.
     */
    public final boolean isSet(InternalThreadLocalMap threadLocalMap) {
        return threadLocalMap != null && threadLocalMap.isIndexedVariableSet(index);
    }
    /**
     * Sets the value to uninitialized; a proceeding call to get() will trigger a call to initialValue().
     */
    public final void remove() {
        remove(InternalThreadLocalMap.getIfSet());
    }

    /**
     * Sets the value to uninitialized for the specified thread local map;
     * a proceeding call to get() will trigger a call to initialValue().
     * The specified thread local map must be for the current thread.
     */
    @SuppressWarnings("unchecked")
    public final void remove(InternalThreadLocalMap threadLocalMap) {
        if (threadLocalMap == null) {
            return;
        }

        // 从数组中移除：重置为UNSET对象
        Object v = threadLocalMap.removeIndexedVariable(index);
        removeFromVariablesToRemove(threadLocalMap, this);

        if (v != InternalThreadLocalMap.UNSET) {
            try {
                // 由子类实现
                onRemoval((V) v);
            } catch (Exception e) {
                PlatformDependent.throwException(e);
            }
        }
    }

    /**
     * 返回线程内部共享的初始化值，子类可以重载
     * Returns the initial value for this thread-local variable.
     */
    protected V initialValue() throws Exception {
        return null;
    }

    /**
     * Invoked when this thread local variable is removed by {@link #remove()}. Be aware that {@link #remove()}
     * is not guaranteed to be called when the `Thread` completes which means you can not depend on this for
     * cleanup of the resources in the case of `Thread` completion.
     */
    protected void onRemoval(@SuppressWarnings("UnusedParameters") V value) throws Exception { }
}
