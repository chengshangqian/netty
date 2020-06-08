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

package io.netty.util.internal;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 用于为Netty中以及所有{@link FastThreadLocal}示例存储线程局部变量的内部数据结构。
 * 注意，这个类设计用于内部使用且随时可能改变。 在你明确直到自己要干什么的情况下使用{@link FastThreadLocal}
 *
 * 每个线程，只能创建或绑定一个InternalThreadLocalMap，或绑定在FastThreadLocalThread线程上，或存储在JDK实现线程区隔的ThreadLocal对象中
 *
 * is subject to change 有改变的可能
 *
 * The internal data structure that stores the thread-local variables for Netty and all {@link FastThreadLocal}s.
 * Note that this class is for internal use only and is subject to change at any time.  Use {@link FastThreadLocal}
 * unless you know what you are doing.
 */
public final class InternalThreadLocalMap extends UnpaddedInternalThreadLocalMap {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(InternalThreadLocalMap.class);

    private static final int DEFAULT_ARRAY_LIST_INITIAL_CAPACITY = 8;
    private static final int STRING_BUILDER_INITIAL_SIZE;
    private static final int STRING_BUILDER_MAX_SIZE;
    private static final int HANDLER_SHARABLE_CACHE_INITIAL_CAPACITY = 4;
    private static final int INDEXED_VARIABLE_TABLE_INITIAL_SIZE = 32;

    /**
     * 未设置对象，占位用的对象，表示该位置未设置或填充过元素
     */
    public static final Object UNSET = new Object();

    private BitSet cleanerFlags;

    static {
        STRING_BUILDER_INITIAL_SIZE =
                SystemPropertyUtil.getInt("io.netty.threadLocalMap.stringBuilder.initialSize", 1024);
        logger.debug("-Dio.netty.threadLocalMap.stringBuilder.initialSize: {}", STRING_BUILDER_INITIAL_SIZE);

        STRING_BUILDER_MAX_SIZE = SystemPropertyUtil.getInt("io.netty.threadLocalMap.stringBuilder.maxSize", 1024 * 4);
        logger.debug("-Dio.netty.threadLocalMap.stringBuilder.maxSize: {}", STRING_BUILDER_MAX_SIZE);
    }

    /**
     * 获取线程局部映射
     *
     * @return
     */
    public static InternalThreadLocalMap getIfSet() {
        // 获取当前线程：从这里开始区隔线程
        Thread thread = Thread.currentThread();

        // 如果当前线程是Netty的FastThreadLocalThread类型线程
        if (thread instanceof FastThreadLocalThread) {
            // 返回线程内部的线程局部映射
            return ((FastThreadLocalThread) thread).threadLocalMap();
        }

        // 返回JDK的线程局部映射
        return slowThreadLocalMap.get();
    }

    /**
     * 获取/创建线程局部映射
     * InternalThreadLocalMap的实例，
     * 要么绑定在FastThreadLocalThread线程上，要么存储在JDK实现线程区隔的ThreadLocal对象中
     *
     * @return
     */
    public static InternalThreadLocalMap get() {
        // 获取当前线程：从这里开始与线程相关，即InternalThreadLocalMap是绑定线程的，相同线程只有一个实例。
        // 这是实现线程隔离的关键。
        Thread thread = Thread.currentThread();

        // 如果当前线程是Netty的FastThreadLocalThread类型线程
        if (thread instanceof FastThreadLocalThread) {
            // 不存在将创建一个Netty的InternalThreadLocalMap实例
            return fastGet((FastThreadLocalThread) thread);
        } else {
            // 不存在将创建一个JDK的线程局部映射
            return slowGet();
        }
    }

    /**
     * 创建一个Netty的InternalThreadLocalMap实例
     *
     * @param thread
     * @return
     */
    private static InternalThreadLocalMap fastGet(FastThreadLocalThread thread) {
        // 获取FastThreadLocalThread类型的【当前线程】的线程局部映射集合
        InternalThreadLocalMap threadLocalMap = thread.threadLocalMap();

        // 如果不存在则创建，并放入到线程中
        if (threadLocalMap == null) {
            thread.setThreadLocalMap(threadLocalMap = new InternalThreadLocalMap());
        }

        // 返回InternalThreadLocalMap实例
        return threadLocalMap;
    }

    /**
     * 获取或创建使用JDK的ThreadLocal持有的InternalThreadLocalMap实例
     *
     * @return
     */
    private static InternalThreadLocalMap slowGet() {
        // 在缓存中获取全局（子类共享）的一个JDK的ThreadLocal对象实例中存取，ThreadLocal本身也是线程区隔的，但性能较慢
        ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = UnpaddedInternalThreadLocalMap.slowThreadLocalMap;

        // 从JDK的ThreadLocal对象实例slowThreadLocalMap中获取InternalThreadLocalMap实例
        InternalThreadLocalMap ret = slowThreadLocalMap.get();

        // 如果不存在则创建并放入slowThreadLocalMap
        if (ret == null) {
            ret = new InternalThreadLocalMap();
            slowThreadLocalMap.set(ret);
        }

        // 返回新创建的InternalThreadLocalMap实例
        return ret;
    }

    public static void remove() {
        Thread thread = Thread.currentThread();
        if (thread instanceof FastThreadLocalThread) {
            ((FastThreadLocalThread) thread).setThreadLocalMap(null);
        } else {
            slowThreadLocalMap.remove();
        }
    }

    public static void destroy() {
        slowThreadLocalMap.remove();
    }

    /**
     * 下一个变量的索引
     *
     * @return
     */
    public static int nextVariableIndex() {
        // 获取后自增下一个变量的原子索引
        int index = nextIndex.getAndIncrement();

        // 如果小于0，恢复原值后抛出异常
        if (index < 0) {
            nextIndex.decrementAndGet();
            // 线程内部索引变量太多
            throw new IllegalStateException("too many thread-local indexed variables");
        }

        // 返回索引
        return index;
    }

    public static int lastVariableIndex() {
        return nextIndex.get() - 1;
    }

    // Cache line padding (must be public)
    // With CompressedOops enabled, an instance of this class should occupy at least 128 bytes.
    public long rp1, rp2, rp3, rp4, rp5, rp6, rp7, rp8, rp9;

    /**
     * 不允许外部创建InternalThreadLocalMap对象
     * 每个线程，只能创建或绑定一个InternalThreadLocalMap实例，
     * 新建后的实例，要么绑定在FastThreadLocalThread线程上，要么存储在JDK实现线程区隔的ThreadLocal对象中
     */
    private InternalThreadLocalMap() {
        super(newIndexedVariableTable());
    }

    /**
     * 创建可索引的变量表（数组），默认是32个元素，
     * 后期会自动扩容
     *
     * @return
     */
    private static Object[] newIndexedVariableTable() {
        // 创建对象数组
        Object[] array = new Object[INDEXED_VARIABLE_TABLE_INITIAL_SIZE];
        // 填充UNSET对象，标识当前为止还未填充过内容
        Arrays.fill(array, UNSET);

        return array;
    }

    public int size() {
        int count = 0;

        if (futureListenerStackDepth != 0) {
            count ++;
        }
        if (localChannelReaderStackDepth != 0) {
            count ++;
        }
        if (handlerSharableCache != null) {
            count ++;
        }
        if (counterHashCode != null) {
            count ++;
        }
        if (random != null) {
            count ++;
        }
        if (typeParameterMatcherGetCache != null) {
            count ++;
        }
        if (typeParameterMatcherFindCache != null) {
            count ++;
        }
        if (stringBuilder != null) {
            count ++;
        }
        if (charsetEncoderCache != null) {
            count ++;
        }
        if (charsetDecoderCache != null) {
            count ++;
        }
        if (arrayList != null) {
            count ++;
        }

        for (Object o: indexedVariables) {
            if (o != UNSET) {
                count ++;
            }
        }

        // We should subtract 1 from the count because the first element in 'indexedVariables' is reserved
        // by 'FastThreadLocal' to keep the list of 'FastThreadLocal's to remove on 'FastThreadLocal.removeAll()'.
        return count - 1;
    }

    public StringBuilder stringBuilder() {
        StringBuilder sb = stringBuilder;
        if (sb == null) {
            return stringBuilder = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);
        }
        if (sb.capacity() > STRING_BUILDER_MAX_SIZE) {
            sb.setLength(STRING_BUILDER_INITIAL_SIZE);
            sb.trimToSize();
        }
        sb.setLength(0);
        return sb;
    }

    public Map<Charset, CharsetEncoder> charsetEncoderCache() {
        Map<Charset, CharsetEncoder> cache = charsetEncoderCache;
        if (cache == null) {
            charsetEncoderCache = cache = new IdentityHashMap<Charset, CharsetEncoder>();
        }
        return cache;
    }

    public Map<Charset, CharsetDecoder> charsetDecoderCache() {
        Map<Charset, CharsetDecoder> cache = charsetDecoderCache;
        if (cache == null) {
            charsetDecoderCache = cache = new IdentityHashMap<Charset, CharsetDecoder>();
        }
        return cache;
    }

    public <E> ArrayList<E> arrayList() {
        return arrayList(DEFAULT_ARRAY_LIST_INITIAL_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public <E> ArrayList<E> arrayList(int minCapacity) {
        ArrayList<E> list = (ArrayList<E>) arrayList;
        if (list == null) {
            arrayList = new ArrayList<Object>(minCapacity);
            return (ArrayList<E>) arrayList;
        }
        list.clear();
        list.ensureCapacity(minCapacity);
        return list;
    }

    public int futureListenerStackDepth() {
        return futureListenerStackDepth;
    }

    public void setFutureListenerStackDepth(int futureListenerStackDepth) {
        this.futureListenerStackDepth = futureListenerStackDepth;
    }

    public ThreadLocalRandom random() {
        ThreadLocalRandom r = random;
        if (r == null) {
            random = r = new ThreadLocalRandom();
        }
        return r;
    }

    public Map<Class<?>, TypeParameterMatcher> typeParameterMatcherGetCache() {
        Map<Class<?>, TypeParameterMatcher> cache = typeParameterMatcherGetCache;
        if (cache == null) {
            typeParameterMatcherGetCache = cache = new IdentityHashMap<Class<?>, TypeParameterMatcher>();
        }
        return cache;
    }

    public Map<Class<?>, Map<String, TypeParameterMatcher>> typeParameterMatcherFindCache() {
        Map<Class<?>, Map<String, TypeParameterMatcher>> cache = typeParameterMatcherFindCache;
        if (cache == null) {
            typeParameterMatcherFindCache = cache = new IdentityHashMap<Class<?>, Map<String, TypeParameterMatcher>>();
        }
        return cache;
    }

    @Deprecated
    public IntegerHolder counterHashCode() {
        return counterHashCode;
    }

    @Deprecated
    public void setCounterHashCode(IntegerHolder counterHashCode) {
        this.counterHashCode = counterHashCode;
    }

    public Map<Class<?>, Boolean> handlerSharableCache() {
        Map<Class<?>, Boolean> cache = handlerSharableCache;
        if (cache == null) {
            // Start with small capacity to keep memory overhead as low as possible.
            handlerSharableCache = cache = new WeakHashMap<Class<?>, Boolean>(HANDLER_SHARABLE_CACHE_INITIAL_CAPACITY);
        }
        return cache;
    }

    public int localChannelReaderStackDepth() {
        return localChannelReaderStackDepth;
    }

    public void setLocalChannelReaderStackDepth(int localChannelReaderStackDepth) {
        this.localChannelReaderStackDepth = localChannelReaderStackDepth;
    }

    /**
     * 检索数组中该位置线程共享对象
     *
     * @param index
     * @return
     */
    public Object indexedVariable(int index) {
        Object[] lookup = indexedVariables;
        return index < lookup.length? lookup[index] : UNSET;
    }

    /**
     * 设置线程内共享的对象
     *
     * @return {@code true} if and only if a new thread-local variable has been created
     */
    public boolean setIndexedVariable(int index, Object value) {
        Object[] lookup = indexedVariables;

        // 容量够大，直接存储
        if (index < lookup.length) {
            Object oldValue = lookup[index];
            lookup[index] = value;

            // 原来的对象应该是未设置，返回false表示覆盖了之前设置的对象，true表示首次设置
            return oldValue == UNSET;
        } else {
            // 扩容存储
            expandIndexedVariableTableAndSet(index, value);
            return true;
        }
    }

    /**
     * 扩容
     *
     * @param index
     * @param value
     */
    private void expandIndexedVariableTableAndSet(int index, Object value) {
        Object[] oldArray = indexedVariables;
        final int oldCapacity = oldArray.length;
        int newCapacity = index;
        newCapacity |= newCapacity >>>  1;
        newCapacity |= newCapacity >>>  2;
        newCapacity |= newCapacity >>>  4;
        newCapacity |= newCapacity >>>  8;
        newCapacity |= newCapacity >>> 16;
        newCapacity ++;

        Object[] newArray = Arrays.copyOf(oldArray, newCapacity);
        Arrays.fill(newArray, oldCapacity, newArray.length, UNSET);
        newArray[index] = value;
        indexedVariables = newArray;
    }

    /**
     * 从数组中移除：重置为UNSET对象
     *
     * @param index
     * @return
     */
    public Object removeIndexedVariable(int index) {
        Object[] lookup = indexedVariables;
        if (index < lookup.length) {
            Object v = lookup[index];
            // 重置为UNSET对象
            lookup[index] = UNSET;
            return v;
        } else {
            return UNSET;
        }
    }

    public boolean isIndexedVariableSet(int index) {
        Object[] lookup = indexedVariables;
        return index < lookup.length && lookup[index] != UNSET;
    }

    public boolean isCleanerFlagSet(int index) {
        return cleanerFlags != null && cleanerFlags.get(index);
    }

    public void setCleanerFlag(int index) {
        if (cleanerFlags == null) {
            cleanerFlags = new BitSet();
        }
        cleanerFlags.set(index);
    }
}
