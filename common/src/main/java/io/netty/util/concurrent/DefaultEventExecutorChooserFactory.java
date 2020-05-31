/*
 * Copyright 2016 The Netty Project
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

import io.netty.util.internal.UnstableApi;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缺省事件执行器选择器工厂类，使用简单循环算法（round-robin）选择下一个{@link EventExecutor}
 * Default implementation which uses simple round-robin to choose next {@link EventExecutor}.
 */
@UnstableApi
public final class DefaultEventExecutorChooserFactory implements EventExecutorChooserFactory {

    /**
     * 单例
     */
    public static final DefaultEventExecutorChooserFactory INSTANCE = new DefaultEventExecutorChooserFactory();

    /**
     * 不允许在外部实例化
     */
    private DefaultEventExecutorChooserFactory() { }

    /**
     * 创建事件执行器选择器
     *
     * @param executors
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public EventExecutorChooser newChooser(EventExecutor[] executors) {
        // 如果事件执行器数量是2的次幂
        if (isPowerOfTwo(executors.length)) {
            return new PowerOfTwoEventExecutorChooser(executors);
        }
        else {
            return new GenericEventExecutorChooser(executors);
        }
    }

    /**
     * 判断val是否是2的次幂
     *
     * @param val
     * @return
     */
    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    /**
     * 2的次幂事件执行器选择器
     */
    private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {
        // 原子整数
        private final AtomicInteger idx = new AtomicInteger();

        // 事件执行器数组
        private final EventExecutor[] executors;

        /**
         * 初始化2次幂事件执行器选择器：executors的长度为2的次幂时使用次选择器
         *
         * @param executors
         */
        PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        /**
         * 选择下一个事件执行器EventExecutor，遍历算法，都是遍历数组
         * 相当于在一个圆环上不断的下一个
         *
         * @return
         */
        @Override
        public EventExecutor next() {
            // 逐位与运算方式获得下一个事件执行器的索引位置
            // idx.getAndIncrement()方法返回当前值0，然后idx自身+1,相当于普通int类型变量的i++
            // 假设executors.length=8，则
            // 第1次调用next()方法，将返回 0 & 7 的结果即executors[0]
            // 第2次调用next()方法，将返回 1 & 7 的结果即executors[1]
            // ...
            // 第9次调用next()方法，将返回 8 & 7 的结果即executors[0]
            // 第10次调用next()方法，将返回 9 & 7 的结果即executors[1]
            // ...
            // 以此类推
            // 位运算符& 优先级低于 算术运算符-，相当于idx.getAndIncrement() & (executors.length - 1)
            // 关于十进制正负数的位运算：
            // 1.java中十进制的int类型正负整数使用4个字节（即32位）的补码保存。开头第一位表示符号位，0位正数，1位负数
            // 2.十进制正数补码为对应的二进制原码本身，十进制负数补码 = 对应正数（即绝对值）二进制原码符号位之外取反 + 1
            // 3.十进制正负数在计算机上进行“&”与位运算时，即使用两个数的补码进行与运算
            // 比如-3补码转换/表示过程
            // 3 ->  【0】0000000 0000000 00000000 00000011
            // -3 => 除符号位取反【1】1111111 11111111 11111111 11111100 -> 然后+1 【1】1111111 11111111 11111111 11111101
            // 3 & -3 => 【0】0000000 0000000 00000000 00000011 & 【1】1111111 11111111 11111111 11111101 => 【0】0000000 0000000 00000000 00000001 => 1
            // 更多例子，可以运行main方法验证
            // 1 & -1 => 【0】0000000 0000000 00000000 00000001 & 【1】1111111 11111111 11111111 11111111 => 【0】0000000 0000000 00000000 00000001 => 1
            // 2 & -2 => 【0】0000000 0000000 00000000 00000010 & 【1】1111111 11111111 11111111 11111110 => 【0】0000000 0000000 00000000 00000010 => 2
            // 3 & -3 => 【0】0000000 0000000 00000000 00000011 & 【1】1111111 11111111 11111111 11111101 => 【0】0000000 0000000 00000000 00000001 => 1
            // 4 & -4 => 【0】0000000 0000000 00000000 00000100 & 【1】1111111 11111111 11111111 11111100 => 【0】0000000 0000000 00000000 00000100 => 4
            // 5 & -5 => 【0】0000000 0000000 00000000 00000101 & 【1】1111111 11111111 11111111 11111011 => 【0】0000000 0000000 00000000 00000001 => 1
            // 6 & -6 => 【0】0000000 0000000 00000000 00000110 & 【1】1111111 11111111 11111111 11111010 => 【0】0000000 0000000 00000000 00000010 => 2
            // 7 & -7 => 【0】0000000 0000000 00000000 00000111 & 【1】1111111 11111111 11111111 11111001 => 【0】0000000 0000000 00000000 00000001 => 1
            // 8 & -8 => 【0】0000000 0000000 00000000 00001000 & 【1】1111111 11111111 11111111 11111000 => 【0】0000000 0000000 00000000 00001000 => 8
            return executors[idx.getAndIncrement() & executors.length - 1];
        }
    }

    /**
     * 智能事件执行器选择器
     */
    private static final class GenericEventExecutorChooser implements EventExecutorChooser {
        // 原子整数
        private final AtomicInteger idx = new AtomicInteger();

        // 事件执行器数组
        private final EventExecutor[] executors;

        /**
         * 初始化智能事件执行器选择器
         *
         * @param executors
         */
        GenericEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        /**
         * 选择下一个事件执行器，遍历算法，都是遍历数组
         *
         * @return
         */
        @Override
        public EventExecutor next() {
            // 求余（再取绝对值）方式获得下一个事件执行器的索引位置
            // idx.getAndIncrement()方法返回当前值0，然后idx自身+1,相当于普通int类型变量的i++
            // 假设executors.length=8，则
            // 第1次调用next()方法，将返回 0 % 8 的结果即executors[0]
            // 第2次调用next()方法，将返回 1 % 8 的结果即executors[1]
            // ...
            // 第9次调用next()方法，将返回 8 % 8 的结果即executors[0]
            // 第10次调用next()方法，将返回 9 % 8 的结果即executors[1]
            // ...
            // 以此类推
            // 这里为什么要再取一次绝对值?
            return executors[Math.abs(idx.getAndIncrement() % executors.length)];
        }
    }

    public static void main(String[] args) {
        int length = 6;

        for (int i = 0; i < length ; i++) {
            // true true true false true false
            System.out.print( i + "=>" + isPowerOfTwo(i) + " ");
        }

        System.out.println();

        AtomicInteger idx = new AtomicInteger();
        for (int i = 0; i < length ; i++) {
            // true true true false true false
            System.out.print( idx.get() + "=>" + isPowerOfTwo(idx.getAndIncrement()) + " ");
        }

        System.out.println();

        // 三个数是否相等，取决于length的值是否为2的次幂，如果是2的次幂，三个数相等，否则将不等
        length = 8;
        for (int i = 0; i < length ; i++) {
            int calcValue = i % length;
            int absValue = Math.abs(i % length);
            int logicalAndValue = i & length - 1;
            System.out.print( calcValue + ":" + absValue + ":" + logicalAndValue + " ");
        }

        // 【2次幂正数】经过与本身的负数进行位与运算后，依然和自身相等
        System.out.print( 1 & -1);// 1
        System.out.print( 2 & -2);// 2
        System.out.print( 3 & -3);// 1
        System.out.print( 4 & -4);// 4
        System.out.print( 5 & -5);// 1
        System.out.print( 6 & -6);// 2
        System.out.print( 7 & -7);// 1
        System.out.print( 8 & -8);// 8
    }
}
