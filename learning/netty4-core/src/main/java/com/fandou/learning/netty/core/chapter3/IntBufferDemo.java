package com.fandou.learning.netty.core.chapter3;

import java.nio.IntBuffer;

/**
 * IntBuffer整数缓冲区示例
 */
public class IntBufferDemo {
    public static void main(String[] args) {
        /**
         * 分配缓冲区容量大小
         */
        int size = 8;

        /**
         * 分配容量大小为size（存放size个整数）的整数缓冲区：底层实现是堆整数缓冲区HeapIntBuffer
         * 新分配的缓冲区，其当前位置position为0，可读/可写边界limit（界限）为缓冲区的大小即容量capacity。
         * 其底层由一个数组实现数据的保存，对应当前数组索引为0
         * 即调用allocate()方法相当于创建了一个指定大小的数组，并包装为缓冲区对象
         */
        IntBuffer buffer = IntBuffer.allocate(size);

        // buffer.position() 返回缓冲区当前位置（开始操作的位置）
        // 随着数据的写入，position将会递增
        if(0 == buffer.position()){
            System.out.println("当前位置position为 => " + buffer.position()); // 0
        }

        // buffer.capacity() 返回缓冲区容量大小
        if(size == buffer.capacity()){
            System.out.println("分配的缓冲区容量大小为 => " + buffer.capacity()); // 8
        }

        // buffer.limit() 返回当前可读或可写的最大值即界限或边界值：新分配的缓冲区，与容量capacity相等
        if(buffer.capacity() == buffer.limit()){
            System.out.println("当前缓冲区可读/可写为 => " + buffer.limit()); // 8
        }

        // 写入整数：未写满，留下4个位置
        for (int i = 0; i < size - 4; i++) {
            int index = i + 1;
            int value = 2 * index;

            // 顺序写入整数数据，其中position将递增
            buffer.put(value);

            // 打印每次写入数据后buffer的内部属性的变化
            System.out.println("------" + index + "------");
            System.out.println("position => " + buffer.position());
            System.out.println("capacity => " + buffer.capacity());
            System.out.println("limit => " + buffer.limit());
        }

        // buffer.flip()方法会重设缓冲区：position=0，limit=可读边界，即可读的最大整数个数
        // 在开始读取数据前，调用此方法，以便从缓冲区头部开始读取数据
        buffer.flip();
        // 打印每次写入数据后buffer的内部属性的变化
        System.out.println("-------调用flip()方法后------");
        System.out.println("position => " + buffer.position()); // 0
        System.out.println("capacity => " + buffer.capacity()); // 8
        System.out.println("limit => " + buffer.limit());  // limit = size - 4

        System.out.println("-------调用get()方法读取后打印------");
        // buffer.hasRemaining()方法检查当前位置position到边界位置limit之间是否还有可读数据/元素
        // 如果有，继续读取
        while(buffer.hasRemaining()){
            // 从缓冲中读取一个整数（因为是整数缓冲区）
            // 调用buffer.get()方法，当前位置position会递增+1
            int value = buffer.get();

            // 打印读取的数据
            System.out.print(value + " ");
        }

        // 打印读取后的Buffer内部属性变量状态
        System.out.println("\n-------调用get()方法后------");
        System.out.println("position => " + buffer.position()); // 4
        System.out.println("capacity => " + buffer.capacity()); // 8
        System.out.println("limit => " + buffer.limit()); // 4

        // 在继续写入数据之前，将可写边界设置为容量大小，否则将抛出缓冲区溢出异常BufferOverflowException
        buffer.limit(buffer.capacity());

        // 将剩下的位置写满
        for (int i = size - 4; i < size ; i++) {
            int index = i + 1;
            int value = 2 * index;

            // 顺序写入整数数据，其中position将递增1
            buffer.put(value);

            // 打印每次写入数据后buffer的内部属性的变化
            System.out.println("------" + index + "------");
            System.out.println("position => " + buffer.position());
            System.out.println("capacity => " + buffer.capacity());
            System.out.println("limit => " + buffer.limit());
        }

        System.out.println("-------读取并打印缓冲区buffer中的所有整数------");
        buffer.flip(); // position: 8 -> 0
        while(buffer.hasRemaining()){
            // 调用buffer.get()方法，当前位置position会递增+1
            int value = buffer.get();
            // 打印
            System.out.print(value + " "); // 2 4 6 8 10 12 14 16
        }
    }
}
