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

package io.netty.util;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.util.internal.MathUtil.safeFindNextPositivePowerOfTwo;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Light-weight object pool based on a thread-local stack.
 *
 * @param <T> the type of the pooled object
 */
public abstract class Recycler<T> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Recycler.class);

    @SuppressWarnings("rawtypes")
    private static final Handle NOOP_HANDLE = new Handle() {
        @Override
        public void recycle(Object object) {
            // NOOP
        }
    };
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(Integer.MIN_VALUE);

    /**
     * 回收对象归属线程ID
     */
    private static final int OWN_THREAD_ID = ID_GENERATOR.getAndIncrement();

    /**
     * 每个线程缺省最大回收容量初始值
     */
    private static final int DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD = 4 * 1024; // Use 4k instances as default.

    /**
     * 每个线程缺省最大回收容量 32768
     */
    private static final int DEFAULT_MAX_CAPACITY_PER_THREAD;

    /**
     * 初始容量
     */
    private static final int INITIAL_CAPACITY;

    /**
     * 最大共享容量系数 2
     */
    private static final int MAX_SHARED_CAPACITY_FACTOR;

    /**
     * 每个线程的最大延迟队列数
     */
    private static final int MAX_DELAYED_QUEUES_PER_THREAD;

    /**
     * 连接容量
     */
    private static final int LINK_CAPACITY;

    /**
     * 比率，占比
     */
    private static final int RATIO;

    /**
     * 延迟队列数占比
     */
    private static final int DELAYED_QUEUE_RATIO;

    static {
        // In the future, we might have different maxCapacity for different object types.
        // e.g. io.netty.recycler.maxCapacity.writeTask
        //      io.netty.recycler.maxCapacity.outboundBuffer
        int maxCapacityPerThread = SystemPropertyUtil.getInt("io.netty.recycler.maxCapacityPerThread",
                SystemPropertyUtil.getInt("io.netty.recycler.maxCapacity", DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD));
        if (maxCapacityPerThread < 0) {
            maxCapacityPerThread = DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD;
        }

        DEFAULT_MAX_CAPACITY_PER_THREAD = maxCapacityPerThread;

        MAX_SHARED_CAPACITY_FACTOR = max(2,
                SystemPropertyUtil.getInt("io.netty.recycler.maxSharedCapacityFactor",
                        2));

        MAX_DELAYED_QUEUES_PER_THREAD = max(0,
                SystemPropertyUtil.getInt("io.netty.recycler.maxDelayedQueuesPerThread",
                        // We use the same value as default EventLoop number
                        NettyRuntime.availableProcessors() * 2));

        LINK_CAPACITY = safeFindNextPositivePowerOfTwo(
                max(SystemPropertyUtil.getInt("io.netty.recycler.linkCapacity", 16), 16));

        // 默认情况下，我们允许每8次尝试一次推送一个回收站处理以前从未回收过的句柄（DefaultHandle）。
        // 这将有助于缓慢增加回收能力，同时不太敏感的分配爆发。
        // By default we allow one push to a Recycler for each 8th try on handles that were never recycled before.
        // This should help to slowly increase the capacity of the recycler while not be too sensitive to allocation
        // bursts.
        RATIO = max(0, SystemPropertyUtil.getInt("io.netty.recycler.ratio", 8));
        DELAYED_QUEUE_RATIO = max(0, SystemPropertyUtil.getInt("io.netty.recycler.delayedQueue.ratio", RATIO));

        if (logger.isDebugEnabled()) {
            if (DEFAULT_MAX_CAPACITY_PER_THREAD == 0) {
                logger.debug("-Dio.netty.recycler.maxCapacityPerThread: disabled");
                logger.debug("-Dio.netty.recycler.maxSharedCapacityFactor: disabled");
                logger.debug("-Dio.netty.recycler.linkCapacity: disabled");
                logger.debug("-Dio.netty.recycler.ratio: disabled");
                logger.debug("-Dio.netty.recycler.delayedQueue.ratio: disabled");
            } else {
                logger.debug("-Dio.netty.recycler.maxCapacityPerThread: {}", DEFAULT_MAX_CAPACITY_PER_THREAD);
                logger.debug("-Dio.netty.recycler.maxSharedCapacityFactor: {}", MAX_SHARED_CAPACITY_FACTOR);
                logger.debug("-Dio.netty.recycler.linkCapacity: {}", LINK_CAPACITY);
                logger.debug("-Dio.netty.recycler.ratio: {}", RATIO);
                logger.debug("-Dio.netty.recycler.delayedQueue.ratio: {}", DELAYED_QUEUE_RATIO);
            }
        }

        INITIAL_CAPACITY = min(DEFAULT_MAX_CAPACITY_PER_THREAD, 256);
    }

    private final int maxCapacityPerThread;
    private final int maxSharedCapacityFactor;
    private final int interval;
    private final int maxDelayedQueuesPerThread;
    private final int delayedQueueInterval;

    private final FastThreadLocal<Stack<T>> threadLocal = new FastThreadLocal<Stack<T>>() {
        @Override
        protected Stack<T> initialValue() {
            return new Stack<T>(Recycler.this, Thread.currentThread(), maxCapacityPerThread, maxSharedCapacityFactor,
                    interval, maxDelayedQueuesPerThread, delayedQueueInterval);
        }

        @Override
        protected void onRemoval(Stack<T> value) {
            // Let us remove the WeakOrderQueue from the WeakHashMap directly if its safe to remove some overhead
            if (value.threadRef.get() == Thread.currentThread()) {
               if (DELAYED_RECYCLED.isSet()) {
                   DELAYED_RECYCLED.get().remove(value);
               }
            }
        }
    };

    protected Recycler() {
        this(DEFAULT_MAX_CAPACITY_PER_THREAD);
    }

    protected Recycler(int maxCapacityPerThread) {
        this(maxCapacityPerThread, MAX_SHARED_CAPACITY_FACTOR);
    }

    protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor) {
        this(maxCapacityPerThread, maxSharedCapacityFactor, RATIO, MAX_DELAYED_QUEUES_PER_THREAD);
    }

    protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor,
                       int ratio, int maxDelayedQueuesPerThread) {
        this(maxCapacityPerThread, maxSharedCapacityFactor, ratio, maxDelayedQueuesPerThread,
                DELAYED_QUEUE_RATIO);
    }

    protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor,
                       int ratio, int maxDelayedQueuesPerThread, int delayedQueueRatio) {
        interval = max(0, ratio);
        delayedQueueInterval = max(0, delayedQueueRatio);
        if (maxCapacityPerThread <= 0) {
            this.maxCapacityPerThread = 0;
            this.maxSharedCapacityFactor = 1;
            this.maxDelayedQueuesPerThread = 0;
        } else {
            this.maxCapacityPerThread = maxCapacityPerThread;
            this.maxSharedCapacityFactor = max(1, maxSharedCapacityFactor);
            this.maxDelayedQueuesPerThread = max(0, maxDelayedQueuesPerThread);
        }
    }

    @SuppressWarnings("unchecked")
    public final T get() {

        // 每个线程最大容量为0，说明不回收对象，也以为着没有回收对象可用
        if (maxCapacityPerThread == 0) {
            return newObject((Handle<T>) NOOP_HANDLE);
        }

        // 获取当前线程的对象T实例的回收栈
        Stack<T> stack = threadLocal.get();

        // 弹出持有者
        DefaultHandle<T> handle = stack.pop();

        // 首次应为null
        if (handle == null) {
            // 创建一个持有者
            handle = stack.newHandle();

            // 持有的对象者指向先创建的对象
            handle.value = newObject(handle);
        }

        // 如果存在该对象的持有者，返回持有的对象是实例
        return (T) handle.value;
    }

    /**
     * @deprecated use {@link Handle#recycle(Object)}.
     */
    @Deprecated
    public final boolean recycle(T o, Handle<T> handle) {
        if (handle == NOOP_HANDLE) {
            return false;
        }

        DefaultHandle<T> h = (DefaultHandle<T>) handle;
        if (h.stack.parent != this) {
            return false;
        }

        h.recycle(o);
        return true;
    }

    final int threadLocalCapacity() {
        return threadLocal.get().elements.length;
    }

    final int threadLocalSize() {
        return threadLocal.get().size;
    }

    protected abstract T newObject(Handle<T> handle);

    public interface Handle<T> extends ObjectPool.Handle<T>  { }

    private static final class DefaultHandle<T> implements Handle<T> {
        /**
         * 最后回收id
         */
        int lastRecycledId;

        /**
         * 回收id
         */
        int recycleId;

        /**
         * 已经回收
         */
        boolean hasBeenRecycled;

        /**
         * 存放回收对象的栈
         */
        Stack<?> stack;

        /**
         * 被回收的对象
         */
        Object value;

        /**
         * 指定一个对象回收栈创建缺省的处理实例
         *
         * @param stack 对象回收栈
         */
        DefaultHandle(Stack<?> stack) {
            this.stack = stack;
        }

        /**
         * 回收对象
         *
         * @param object 被回收的目标对象
         */
        @Override
        public void recycle(Object object) {
            if (object != value) {
                throw new IllegalArgumentException("object does not belong to handle");
            }

            // 获取绑定的回收栈
            Stack<?> stack = this.stack;

            if (lastRecycledId != recycleId || stack == null) {
                throw new IllegalStateException("recycled already");
            }

            // 放入回收栈
            stack.push(this);
        }
    }

    private static final FastThreadLocal<Map<Stack<?>, WeakOrderQueue>> DELAYED_RECYCLED =
            new FastThreadLocal<Map<Stack<?>, WeakOrderQueue>>() {
        @Override
        protected Map<Stack<?>, WeakOrderQueue> initialValue() {
            return new WeakHashMap<Stack<?>, WeakOrderQueue>();
        }
    };

    /**
     * 弱序队列
     * 一种队列，它只对可见性作适度的保证：里面储存的内容（线程）按正确的顺序显示，但我们不能绝对保证看到任何东西，因此保持队列的维护成本很低
     */
    // a queue that makes only moderate guarantees about visibility: items are seen in the correct order,
    // but we aren't absolutely guaranteed to ever see anything at all, thereby keeping the queue cheap to maintain
    private static final class WeakOrderQueue extends WeakReference<Thread> {

        /**
         * 创建一个若序队列
         */
        static final WeakOrderQueue DUMMY = new WeakOrderQueue();

        // 原子连接，用于写索引
        // Let Link extend AtomicInteger for intrinsics. The Link itself will be used as writerIndex.
        @SuppressWarnings("serial")
        static final class Link extends AtomicInteger {
            // 回收对象持有者数组
            final DefaultHandle<?>[] elements = new DefaultHandle[LINK_CAPACITY];

            // 读索引
            int readIndex;

            // 写一个写索引?
            Link next;
        }

        // Its important this does not hold any reference to either Stack or WeakOrderQueue.
        private static final class Head {
            private final AtomicInteger availableSharedCapacity;

            Link link;

            Head(AtomicInteger availableSharedCapacity) {
                this.availableSharedCapacity = availableSharedCapacity;
            }

            /**
             * Reclaim all used space and also unlink the nodes to prevent GC nepotism.
             */
            void reclaimAllSpaceAndUnlink() {
                Link head = link;
                link = null;
                int reclaimSpace = 0;
                while (head != null) {
                    reclaimSpace += LINK_CAPACITY;
                    Link next = head.next;
                    // Unlink to help GC and guard against GC nepotism.
                    head.next = null;
                    head = next;
                }
                if (reclaimSpace > 0) {
                    reclaimSpace(reclaimSpace);
                }
            }

            private void reclaimSpace(int space) {
                availableSharedCapacity.addAndGet(space);
            }

            void relink(Link link) {
                reclaimSpace(LINK_CAPACITY);
                this.link = link;
            }

            /**
             * Creates a new {@link} and returns it if we can reserve enough space for it, otherwise it
             * returns {@code null}.
             */
            Link newLink() {
                return reserveSpaceForLink(availableSharedCapacity) ? new Link() : null;
            }

            static boolean reserveSpaceForLink(AtomicInteger availableSharedCapacity) {
                for (;;) {
                    int available = availableSharedCapacity.get();
                    if (available < LINK_CAPACITY) {
                        return false;
                    }
                    if (availableSharedCapacity.compareAndSet(available, available - LINK_CAPACITY)) {
                        return true;
                    }
                }
            }
        }

        // chain of data items
        private final Head head;
        private Link tail;
        // pointer to another queue of delayed items for the same stack
        private WeakOrderQueue next;
        private final int id = ID_GENERATOR.getAndIncrement();
        private final int interval;
        private int handleRecycleCount;

        private WeakOrderQueue() {
            super(null);
            head = new Head(null);
            interval = 0;
        }

        private WeakOrderQueue(Stack<?> stack, Thread thread) {
            super(thread);
            tail = new Link();

            // Its important that we not store the Stack itself in the WeakOrderQueue as the Stack also is used in
            // the WeakHashMap as key. So just store the enclosed AtomicInteger which should allow to have the
            // Stack itself GCed.
            head = new Head(stack.availableSharedCapacity);
            head.link = tail;
            interval = stack.delayedQueueInterval;
            handleRecycleCount = interval; // Start at interval so the first one will be recycled.
        }

        /**
         * 指定回收栈和线程创建一个弱序队列
         *
         * @param stack
         * @param thread
         * @return
         */
        static WeakOrderQueue newQueue(Stack<?> stack, Thread thread) {
            // We allocated a Link so reserve the space
            // 我们分配了一个链接以便保留空间
            if (!Head.reserveSpaceForLink(stack.availableSharedCapacity)) {
                return null;
            }

            // 为stack和线程thread创建一个弱序队列
            final WeakOrderQueue queue = new WeakOrderQueue(stack, thread);
            // Done outside of the constructor to ensure WeakOrderQueue.this does not escape the constructor and so
            // may be accessed while its still constructed.
            stack.setHead(queue);

            return queue;
        }

        WeakOrderQueue getNext() {
            return next;
        }

        void setNext(WeakOrderQueue next) {
            assert next != this;
            this.next = next;
        }

        void reclaimAllSpaceAndUnlink() {
            head.reclaimAllSpaceAndUnlink();
            this.next = null;
        }

        void add(DefaultHandle<?> handle) {
            handle.lastRecycledId = id;

            // While we also enforce the recycling ratio one we transfer objects from the WeakOrderQueue to the Stack
            // we better should enforce it as well early. Missing to do so may let the WeakOrderQueue grow very fast
            // without control if the Stack
            if (handleRecycleCount < interval) {
                handleRecycleCount++;
                // Drop the item to prevent recycling to aggressive.
                return;
            }
            handleRecycleCount = 0;

            Link tail = this.tail;
            int writeIndex;
            if ((writeIndex = tail.get()) == LINK_CAPACITY) {
                Link link = head.newLink();
                if (link == null) {
                    // Drop it.
                    return;
                }
                // We allocate a Link so reserve the space
                this.tail = tail = tail.next = link;

                writeIndex = tail.get();
            }
            tail.elements[writeIndex] = handle;
            handle.stack = null;
            // we lazy set to ensure that setting stack to null appears before we unnull it in the owning thread;
            // this also means we guarantee visibility of an element in the queue if we see the index updated
            tail.lazySet(writeIndex + 1);
        }

        /**
         * 有最终的数据
         *
         * @return
         */
        boolean hasFinalData() {
            return tail.readIndex != tail.get();
        }

        // 将尽可能多的项目（回收的对象）从该队列传输到指定的堆栈，如果传输了任何回收的对象，则返回true
        // transfer as many items as we can from this queue to the stack, returning true if any were transferred
        @SuppressWarnings("rawtypes")
        boolean transfer(Stack<?> dst) {
            Link head = this.head.link;
            if (head == null) {
                return false;
            }

            if (head.readIndex == LINK_CAPACITY) {
                if (head.next == null) {
                    return false;
                }
                head = head.next;
                this.head.relink(head);
            }

            final int srcStart = head.readIndex;
            int srcEnd = head.get();
            final int srcSize = srcEnd - srcStart;
            if (srcSize == 0) {
                return false;
            }

            final int dstSize = dst.size;
            final int expectedCapacity = dstSize + srcSize;

            if (expectedCapacity > dst.elements.length) {
                final int actualCapacity = dst.increaseCapacity(expectedCapacity);
                srcEnd = min(srcStart + actualCapacity - dstSize, srcEnd);
            }

            if (srcStart != srcEnd) {
                final DefaultHandle[] srcElems = head.elements;
                final DefaultHandle[] dstElems = dst.elements;
                int newDstSize = dstSize;
                for (int i = srcStart; i < srcEnd; i++) {
                    DefaultHandle<?> element = srcElems[i];
                    if (element.recycleId == 0) {
                        element.recycleId = element.lastRecycledId;
                    } else if (element.recycleId != element.lastRecycledId) {
                        throw new IllegalStateException("recycled already");
                    }
                    srcElems[i] = null;

                    if (dst.dropHandle(element)) {
                        // Drop the object.
                        continue;
                    }
                    element.stack = dst;
                    dstElems[newDstSize ++] = element;
                }

                if (srcEnd == LINK_CAPACITY && head.next != null) {
                    // Add capacity back as the Link is GCed.
                    this.head.relink(head.next);
                }

                head.readIndex = srcEnd;
                if (dst.size == newDstSize) {
                    return false;
                }
                dst.size = newDstSize;
                return true;
            } else {
                // The destination stack is full already.
                return false;
            }
        }
    }

    private static final class Stack<T> {

        // we keep a queue of per-thread queues, which is appended to once only, each time a new thread other
        // than the stack owner recycles: when we run out of items in our stack we iterate this collection
        // to scavenge those that can be reused. this permits us to incur minimal thread synchronisation whilst
        // still recycling all items.
        final Recycler<T> parent;

        // We store the Thread in a WeakReference as otherwise we may be the only ones that still hold a strong
        // Reference to the Thread itself after it died because DefaultHandle will hold a reference to the Stack.
        //
        // The biggest issue is if we do not use a WeakReference the Thread may not be able to be collected at all if
        // the user will store a reference to the DefaultHandle somewhere and never clear this reference (or not clear
        // it in a timely manner).
        // 线程弱序引用,当前回收栈绑定的线程
        final WeakReference<Thread> threadRef;

        final AtomicInteger availableSharedCapacity;
        private final int maxDelayedQueues;

        /**
         * 最大容量
         */
        private final int maxCapacity;

        /**
         * 回收间隔
         */
        private final int interval;

        /**
         * 延迟队列回收间隔
         */
        private final int delayedQueueInterval;

        /**
         * 回收对象持有者数组
         */
        DefaultHandle<?>[] elements;

        /**
         * 回收对象数量
         */
        int size;

        /**
         * 处理回收统计
         */
        private int handleRecycleCount;

        /**
         * WeakOrderQueue游标以及双向链表头和上一个
         */
        private WeakOrderQueue cursor, prev;
        private volatile WeakOrderQueue head;

        Stack(Recycler<T> parent, Thread thread, int maxCapacity, int maxSharedCapacityFactor,
              int interval, int maxDelayedQueues, int delayedQueueInterval) {
            this.parent = parent;

            // 当前回收栈绑定的线程
            threadRef = new WeakReference<Thread>(thread);

            this.maxCapacity = maxCapacity;
            availableSharedCapacity = new AtomicInteger(max(maxCapacity / maxSharedCapacityFactor, LINK_CAPACITY));
            elements = new DefaultHandle[min(INITIAL_CAPACITY, maxCapacity)];
            this.interval = interval;
            this.delayedQueueInterval = delayedQueueInterval;
            handleRecycleCount = interval; // Start at interval so the first one will be recycled.
            this.maxDelayedQueues = maxDelayedQueues;
        }

        // Marked as synchronized to ensure this is serialized.
        synchronized void setHead(WeakOrderQueue queue) {
            queue.setNext(head);
            head = queue;
        }

        int increaseCapacity(int expectedCapacity) {
            int newCapacity = elements.length;
            int maxCapacity = this.maxCapacity;
            do {
                newCapacity <<= 1;
            } while (newCapacity < expectedCapacity && newCapacity < maxCapacity);

            newCapacity = min(newCapacity, maxCapacity);
            if (newCapacity != elements.length) {
                elements = Arrays.copyOf(elements, newCapacity);
            }

            return newCapacity;
        }

        /**
         * 弹出回收对象持有者
         *
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        DefaultHandle<T> pop() {
            // 获取当前回收栈的存储数量
            int size = this.size;

            // 如果为0
            if (size == 0) {
                // 尝试其它线程/异线程回收
                if (!scavenge()) {
                    // 回收失败返回null
                    return null;
                }

                // 从其它线程回收成功，再次检查回收栈的数量
                size = this.size;
                // 如果还是没有可用的回收对象，返回null
                if (size <= 0) {
                    // double check, avoid races
                    return null;
                }
            }

            // 回收栈有可用的对象可用，从回收栈中取出回收对象
            size --;
            DefaultHandle ret = elements[size];
            elements[size] = null;
            // As we already set the element[size] to null we also need to store the updated size before we do
            // any validation. Otherwise we may see a null value when later try to pop again without a new element
            // added before.
            this.size = size;

            if (ret.lastRecycledId != ret.recycleId) {
                throw new IllegalStateException("recycled multiple times");
            }
            ret.recycleId = 0;
            ret.lastRecycledId = 0;
            return ret;
        }

        /**
         * 尝试从其它线程中获取本线程创建但被其它线程回收的对象
         *
         * @return
         */
        private boolean scavenge() {
            // continue an existing scavenge, if any
            // 尝试回收
            if (scavengeSome()) {
                // 回收操作成功
                return true;
            }

            // reset our scavenge cursor
            prev = null;
            cursor = head;
            return false;
        }

        /**
         * 尝试从其它线程中获取本线程创建但被其它线程回收的对象
         *
         * @return
         */
        private boolean scavengeSome() {
            // 上一个弱序队列
            WeakOrderQueue prev;
            // 弱序队列游标：指向当前回收栈（的弱序队列游标）
            WeakOrderQueue cursor = this.cursor;

            // 游标为null
            if (cursor == null) {
                // 设置游标从头节点开始
                prev = null;
                cursor = head;

                // 如果头节点也是null，回收失败：即没有可用的弱序队列（回收对象用的）
                if (cursor == null) {
                    return false;
                }
            }
            // 如游标不会null，即当前栈的游标不为空
            else {
                // 上一个prev之上当前游标的上一个this.prev
                prev = this.prev;
            }

            // 开始尝试回收
            boolean success = false;
            do {
                // transfer成功
                if (cursor.transfer(this)) {
                    success = true;
                    break;
                }

                // transfer不成功，获取下一个弱序队列
                WeakOrderQueue next = cursor.getNext();

                // 下一个弱序队列不存在
                if (cursor.get() == null) {
                    // If the thread associated with the queue is gone, unlink it, after
                    // performing a volatile read to confirm there is no data left to collect.
                    // We never unlink the first queue, as we don't want to synchronize on updating the head.
                    // 如果游标有最终数据，尝试transfer
                    if (cursor.hasFinalData()) {
                        for (;;) {
                            if (cursor.transfer(this)) {
                                success = true;
                            } else {
                                break;
                            }
                        }
                    }

                    // 如果上一个不为null，修正链表指向，把链表正确链接上。将当前为null的cursor代表的弱序队列进行GC？
                    if (prev != null) {
                        // Ensure we reclaim all space before dropping the WeakOrderQueue to be GC'ed.
                        cursor.reclaimAllSpaceAndUnlink();
                        prev.setNext(next);
                    }
                }
                // 下一个弱序队列存在，设置正确prev指向当前游标
                else {
                    prev = cursor;
                }

                // 接着将游标指向下一个弱序队列，继续遍历，直到遍历完链表中的所有弱序队列为止
                cursor = next;

            } while (cursor != null && !success);

            // 遍历完，返回遍历结果
            this.prev = prev;
            this.cursor = cursor;
            return success;
        }

        /**
         * 放入对象回收栈
         *
         * @param item
         */
        void push(DefaultHandle<?> item) {
            // 获取当前线程
            Thread currentThread = Thread.currentThread();

            // 如果当前线程与回收栈绑定线程一致，立即放入回收栈
            if (threadRef.get() == currentThread) {
                // The current Thread is the thread that belongs to the Stack, we can try to push the object now.
                pushNow(item);
            }
            // 如果当前线程和回收栈绑定的线程不一致
            else {
                // The current Thread is not the one that belongs to the Stack
                // (or the Thread that belonged to the Stack was collected already), we need to signal that the push
                // happens later.
                pushLater(item, currentThread);
            }
        }

        /**
         * 立即放入回收栈
         *
         * @param item
         */
        private void pushNow(DefaultHandle<?> item) {
            // 如果不是第一次回收，抛出异常：不允许重复回收
            // 默认持有者recycleId和lastRecycledId都为o
            if ((item.recycleId | item.lastRecycledId) != 0) {
                throw new IllegalStateException("recycled already");
            }

            // 设置回收id
            item.recycleId = item.lastRecycledId = OWN_THREAD_ID;

            // 当前存储的回收对象数量
            int size = this.size;

            // 超过最大容量或是可以暂时不回收的对象（netty设定的回收间隔内暂不做回收，比如8次，则首次回收后，再发起8次才会被回收，中间7次将不回收，避免爆发）
            if (size >= maxCapacity || dropHandle(item)) {
                // Hit the maximum capacity or should drop - drop the possibly youngest object.
                return;
            }

            // 扩容2倍后存储
            if (size == elements.length) {
                elements = Arrays.copyOf(elements, min(size << 1, maxCapacity));
            }

            // 放入存储回收对象的数组中
            elements[size] = item;
            this.size = size + 1;
        }

        /**
         * 当前线程与回收栈绑定的线程不一致时，稍后再放入回收栈
         *
         * @param item 回收对象持有者
         * @param thread 当前线程，与回收栈绑定的不是同一个线程
         */
        private void pushLater(DefaultHandle<?> item, Thread thread) {
            // 不支持跨线程回收
            if (maxDelayedQueues == 0) {
                // We don't support recycling across threads and should just drop the item on the floor.
                return;
            }

            // 我们不想将队列的ref作为弱映射中的值，所以我们将其空出来；
            // 为了确保以后恢复队列时不会出现竞争，我们在这里强制执行内存顺序
            // we don't want to have a ref to the queue as the value in our weak map
            // so we null it out; to ensure there are no races with restoring it later
            // we impose a memory ordering here (no-op on x86)

            // 假设当前线程为thread，而当前回收栈即this绑定的线程为thread-01

            // 获取当前线程thread的回收栈-弱序队列的映射集（延迟回收栈）
            Map<Stack<?>, WeakOrderQueue> delayedRecycled = DELAYED_RECYCLED.get();


            // 尝试从当前线程thread【延迟回收栈】中获取线程thread-01的回收栈即this的弱序队列
            WeakOrderQueue queue = delayedRecycled.get(this);

            // 如果thread-01的弱序队列不存在
            if (queue == null) {
                // 超过最大延迟队列数，则放入WeakOrderQueue.DUMMY弱序队列，将删除该对象
                if (delayedRecycled.size() >= maxDelayedQueues) {
                    // Add a dummy queue so we know we should drop the object
                    delayedRecycled.put(this, WeakOrderQueue.DUMMY);
                    return;
                }

                // 尝试为当前栈this和当前线程thread创建一个弱序队列
                // Check if we already reached the maximum number of delayed queues and if we can allocate at all.
                if ((queue = newWeakOrderQueue(thread)) == null) {
                    // drop object
                    return;
                }

                // 队列创建成功，则将当前栈（归属线程thread-01）和弱序队列放入当前线程thread的回收栈-弱序队列映射中
                delayedRecycled.put(this, queue);
            }

            // 队列为WeakOrderQueue.DUMMY
            else if (queue == WeakOrderQueue.DUMMY) {
                // drop object
                return;
            }

            // 将回收对象添加到队列中
            queue.add(item);
        }

        /**
         * 分配一个新的弱序队列实例，可能返回null如果不成功
         *
         * Allocate a new {@link WeakOrderQueue} or return {@code null} if not possible.
         */
        private WeakOrderQueue newWeakOrderQueue(Thread thread) {
            return WeakOrderQueue.newQueue(this, thread);
        }

        /**
         * 删除一个持有者
         *
         * @param handle
         * @return
         */
        boolean dropHandle(DefaultHandle<?> handle) {
            // 如果持有者还没有被回收过
            if (!handle.hasBeenRecycled) {
                // 计算当前handle对象被发起回收的次数是否在间隔之内，如果是，则暂不回收，interval默认是8
                if (handleRecycleCount < interval) {
                    handleRecycleCount++;
                    // Drop the object.
                    return true;
                }

                // 大于回收间隔，重置
                handleRecycleCount = 0;
                // 设置已被回收（将被回收？）
                handle.hasBeenRecycled = true;
            }
            return false;
        }

        /**
         * 创建一个默认的DefaultHandle实例
         *
         * @return
         */
        DefaultHandle<T> newHandle() {
            return new DefaultHandle<T>(this);
        }
    }
}
