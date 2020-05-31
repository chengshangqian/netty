/*
* Copyright 2017 The Netty Project
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

/**
 * 将任务包装为快速本地线程任务
 */
final class FastThreadLocalRunnable implements Runnable {
    /**
     * 原始任务
     */
    private final Runnable runnable;

    /**
     * 创建FastThreadLocalRunnable实例
     *
     * @param runnable 原始任务runnable
     */
    private FastThreadLocalRunnable(Runnable runnable) {
        // 非空任务
        this.runnable = ObjectUtil.checkNotNull(runnable, "runnable");
    }

    /**
     * 线程start时，将调用原始任务的run方法，执行任务
     */
    @Override
    public void run() {
        try {
            runnable.run();
        } finally {
            // 任务执行后，清除当前线程中的缓存所有内部变量
            // FastThreadLocal类似ThreadLocal，为当前线程上下文，设计给开发人员用来当前存储线程相关的内容
            // TODO 后续详细了解原理
            FastThreadLocal.removeAll();
        }
    }

    /**
     * 将普通的任务Runnable包装为快速本地任务FastThreadLocalRunnable
     *
     * @param runnable 原始的普通任务
     * @return 返回FastThreadLocalRunnable实例
     */
    static Runnable wrap(Runnable runnable) {
        // 如果runnable本身为FastThreadLocalRunnable的实例，则原样返回，否则将分装为一个新的FastThreadLocalRunnable实例返回
        return runnable instanceof FastThreadLocalRunnable ? runnable : new FastThreadLocalRunnable(runnable);
    }
}
