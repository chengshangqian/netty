package com.fandou.learning.netty.core.chapter3;

import org.junit.jupiter.api.Test;

import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class IntBufferTest {
    @Test
    void testIntBuffer(){

        /**
         * 分配缓冲区容量大小
         */
        int size = 8;

        /**
         * 分配容量大小为size（存放size个整数）的整数缓冲区：底层实现是堆整数缓冲区HeapIntBuffer
         * 新分配的缓冲区，其当前位置position为0，可读/可写边界limit（界限）为缓冲区的大小即容量capacity。
         * 其底层由一个数组实现数据的保存，对应当前数组索引为0
         */
        IntBuffer buffer = IntBuffer.allocate(size);

        // 断言新分配的缓冲区，其当前位置position从0开始
        assertEquals(0,buffer.position());

        // 断言新分配的缓冲区，其buffer.capacity()方法返回的容量大小与初始化缓冲区时给定的大小size一致
        assertEquals(size,buffer.capacity());

        // 断言新分配的缓冲区，其可写的边界值大小为缓冲区容量大小一致
        assertEquals(size,buffer.limit());

        // 写入整数：未写满，留下4个位置
        for (int i = 0; i < size - 4; i++) {
            int index = i + 1;
            int value = 2 * index;

            // 写入一个整数到缓冲区，当前位置position将递增+1
            buffer.put(value);

            // 断言每次写入一个整数后，当前位置position递增+1
            assertEquals(index,buffer.position());

            // 容量和可写的边界值不会变化
            assertEquals(size,buffer.capacity());
            assertEquals(size,buffer.limit());
        }

        // buffer.flip()方法会重设缓冲区：当前位置position=0，limit=可读边界，即可读的最大整数个数
        // 在开始读取数据前，调用此方法，以便从缓冲区头部开始读取数据
        buffer.flip();

        // 断言当前位置从变成从0开始，而非之前写入数据后的位置
        assertNotEquals((size - 4),buffer.position());
        assertEquals(0,buffer.position());
        // 容量不会变化
        assertEquals(size,buffer.capacity());
        // limit变为可读边界值：变成写入的整数个数
        int readable = size - 4;
        assertEquals(readable,buffer.limit());

        // 读取并打印buffer中的整数
        // buffer.hasRemaining()方法检查当前位置position到边界位置limit之间是否还有可读数据/元素
        // 如果有，继续读取
        while(buffer.hasRemaining()){
            // 从缓冲中读取一个整数（因为是整数缓冲区）
            // 调用buffer.get()方法，当前位置position会递增+1
            int value = buffer.get();

            // 打印读取的数据
            System.out.print(value + " ");
        }

        // 经过读取操作后，断言当前位置从0 -> readable (size - 4)
        assertEquals(readable,buffer.position());
        assertNotEquals(0,buffer.position());

        // 在继续写入数据之前，将可写边界设置为容量大小，否则将抛出缓冲区溢出异常BufferOverflowException
        buffer.limit(buffer.capacity());

        // 将剩下的位置写满
        for (int i = readable; i < size ; i++) {
            int index = i + 1;
            int value = 2 * index;

            // 顺序写入整数数据，其中position将递增1
            buffer.put(value);

            // 断言每次写入一个整数后，当前位置position递增+1
            assertEquals(index,buffer.position());
        }

        System.out.println("\n-------读取并打印缓冲区buffer中的所有整数------");
        buffer.flip(); // position: 8 -> 0
        assertEquals(0,buffer.position());
        while(buffer.hasRemaining()){
            // 调用buffer.get()方法，当前位置position会递增+1
            int value = buffer.get();
            // 打印
            System.out.print(value + " "); // 2 4 6 8 10 12 14 16
        }

        // 断言完整满的缓冲区数据后，当前位置与边界和缓冲区容量大小一致
        assertEquals(buffer.limit(),buffer.position());
        assertEquals(buffer.capacity(),buffer.position());
    }
}
