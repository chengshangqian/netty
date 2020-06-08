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
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * 线程执行器：每个任务分配一个线程
 */
public final class ThreadPerTaskExecutor implements Executor {
    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadPerTaskExecutor.class);

    /**
     * 线程工厂
     */
    private final ThreadFactory threadFactory;

    /**
     * 指定线程工厂，创建一个ThreadPerTaskExecutor实例
     *
     * @param threadFactory 线程工厂
     */
    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        // 指定的线程不能为null，否则抛出空指针异常NullPointerException
        this.threadFactory = ObjectUtil.checkNotNull(threadFactory, "threadFactory");
    }

    /**
     * 创建一个新的线程，并执行任务command
     *
     * @param command 任务/命令
     */
    @Override
    public void execute(Runnable command) {
        // 使用线程工厂开启一个新/空闲的线程并开始执行任务，此线程工厂为DefaultThreadFactory实例
        // 创建的是快速线程内部变量的线程FastThreadLocalThread实例
        logger.info("使用线程工厂创建新线程...");
        threadFactory.newThread(command).start();
    }
}
