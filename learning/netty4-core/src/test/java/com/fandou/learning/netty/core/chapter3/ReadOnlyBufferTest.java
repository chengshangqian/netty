package com.fandou.learning.netty.core.chapter3;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class ReadOnlyBufferTest {
    @Test
    void testReadOnlyBuffer(){
        // 创建字节缓冲区，容量大小为10，并初始化数据和填满缓冲区
        ByteBuffer sourceByteBuffer = ByteBuffer.allocate(10);
        for (int i = 1; i <= 10; i++) {
            sourceByteBuffer.put((byte)i);
        }

        // 创建只读缓冲区分片（值4，5，6，7）：分片与源缓冲区的数据是共享，对分片或源缓冲区中的修改，两边都一起被修改
        sourceByteBuffer.position(3);
        sourceByteBuffer.limit(7);
        ByteBuffer readonlyByteBuffer = sourceByteBuffer.slice().asReadOnlyBuffer();

        // 读取只读缓冲区数据，并尝试修改
        for (int i = 0; i < readonlyByteBuffer.capacity(); i++) {
            byte value = readonlyByteBuffer.get(i);
            value *= 10;
            try {
                // 尝试修改只读缓冲区，预期将发生只读缓冲区异常ReadOnlyBufferException
                readonlyByteBuffer.put(i, value);
                // 如果能够修改不被抛出异常，断言失败
                fail();
            }
            catch (Exception ex){
                System.out.println("不支持对只读缓冲区进行写操作.");
            }
        }

        // 修改源缓冲区中与分片重叠的其中一个数据
        sourceByteBuffer.put(5, (byte) (sourceByteBuffer.get(5) * 10));

        // 断言两个缓冲区对应的数据是相等的：即两个缓冲区的数据是共享的
        assertEquals(sourceByteBuffer.get(5),readonlyByteBuffer.get(5-3));

        // 打印源缓冲区的数据
        sourceByteBuffer.position(0);
        sourceByteBuffer.limit(sourceByteBuffer.capacity());
        while (sourceByteBuffer.hasRemaining()){
            System.out.print(sourceByteBuffer.get() + " ");
        }
    }
}
