package com.fandou.learning.netty.core.chapter3;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class BufferSliceTest {
    @Test
    void testBufferSlice(){
        // 创建字节缓冲区，容量大小为10，并初始化数据和填满缓冲区
        ByteBuffer sourceByteBuffer = ByteBuffer.allocate(10);
        for (int i = 1; i <= 10; i++) {
            sourceByteBuffer.put((byte)i);
        }

        // 创建缓冲区分片（值4，5，6，7）：分片与源缓冲区的数据是共享，对分片或源缓冲区中的修改，两边都一起被修改
        sourceByteBuffer.position(3);
        sourceByteBuffer.limit(7);
        ByteBuffer sliceByteBuffer = sourceByteBuffer.slice();

        // 修改分片中的数据
        for (int i = 0; i < sliceByteBuffer.capacity(); i++) {
            byte value = sliceByteBuffer.get(i);
            value *= 10;
            sliceByteBuffer.put(i,value);
        }

        // 修改源缓冲区中与分片重叠的其中一个数据
        sourceByteBuffer.put(5, (byte) (sourceByteBuffer.get(5) + 5));

        // 断言两个缓冲区对应的数据是相等的
        for (int i = 3; i < 7; i++) {
            int sourceByteBufferIndex = i;
            int sliceByteBufferIndex = i-3;
            assertEquals(sourceByteBuffer.get(sourceByteBufferIndex),sliceByteBuffer.get(sliceByteBufferIndex));
        }

        // 打印源缓冲区的数据
        sourceByteBuffer.position(0);
        sourceByteBuffer.limit(sourceByteBuffer.capacity());
        while (sourceByteBuffer.hasRemaining()){
            System.out.print(sourceByteBuffer.get() + " ");
        }
    }
}
