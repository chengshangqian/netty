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
package io.netty.channel.nio;

import java.nio.channels.SelectionKey;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 可选择的SelectionKey集合类，Netty对NIO SelectionKey的封装
 */
final class SelectedSelectionKeySet extends AbstractSet<SelectionKey> {

    /**
     * SelectionKey数组
     */
    SelectionKey[] keys;

    /**
     * SelectionKey数组当前的元素个数
     */
    int size;

    /**
     * 创建SelectedSelectionKeySet实例，初始化SelectionKey数组大小为1024
     */
    SelectedSelectionKeySet() {
        keys = new SelectionKey[1024];
    }

    /**
     * 添加一个SelectionKey到数组中
     *
     * @param o SelectionKey实例
     * @return
     */
    @Override
    public boolean add(SelectionKey o) {
        if (o == null) {
            return false;
        }

        // 添加到一个SelectionKey元素到数组中，元素个数size+1
        keys[size++] = o;

        // 检查数组剩余可用位置，如果数组已经填满，则对数组进行扩容
        if (size == keys.length) {
            // 对数组扩容，每次扩大为原来的2倍
            increaseCapacity();
        }

        return true;
    }

    /**
     * 移除
     *
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        return false;
    }

    /**
     * 包含
     *
     * @param o
     * @return
     */
    @Override
    public boolean contains(Object o) {
        return false;
    }

    /**
     * 返回数组当前已填充的元素个数
     *
     * @return
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * SelectionKey数组迭代器
     *
     * @return
     */
    @Override
    public Iterator<SelectionKey> iterator() {
        // 返回SelectionKey迭代器
        return new Iterator<SelectionKey>() {
            private int idx;

            @Override
            public boolean hasNext() {
                return idx < size;
            }

            @Override
            public SelectionKey next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return keys[idx++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * 重置数组
     */
    void reset() {
        reset(0);
    }

    /**
     * 重置数组
     *
     * @param start
     */
    void reset(int start) {
        Arrays.fill(keys, start, size, null);
        size = 0;
    }

    /**
     * 扩大SelectionKey数组容量
     *
     * 数组大小每次扩大为原来的2倍：x进制数左移1位，表示原来的数字的x倍(即原来的大小乘以x)，反之表示为原来的1/x倍(即原来的大小除以x)
     * 左移：进制乘法，右移：进制除法，每移1位就是x进制数的1个次方
     */
    private void increaseCapacity() {
        // 创建新的数组，新数组大小位原来数组的2倍（左移1位位2的1次方即2，左移2位，即2的2次方则为4）
        SelectionKey[] newKeys = new SelectionKey[keys.length << 1];

        // 将原来的数组元素拷贝到新数组中
        System.arraycopy(keys, 0, newKeys, 0, size);

        // 更新原有数组的引用指向新数组
        keys = newKeys;
    }
}
