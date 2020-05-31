package com.fandou.learning.netty.core.chapter3;

import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class DirectBufferTest {
    @Test
    void testDirectBuffer(){
        // 创建直接缓冲区：不会创建JVM中的数据来保存数据，而是交由操作系统去创建和管理，直接缓冲区属于内核空间即操作系统的内存缓冲区
        ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(10);

        // 直接缓冲区进行数据写入
        for (int i = 0; i < 10; i++) {
            directByteBuffer.put((byte) i);
        }

        // 创建一个数组，尝试保存直接缓冲区中的数组
        byte[] dst;

        try{
            // 尝试像JVM堆缓冲区一样引用创建的数据，将抛出不支持操作异常UnsupportedOperationException
            dst = directByteBuffer.array();
            // 如果没有抛出异常，断言失败
            fail();
        }
        catch (Exception ex){
            System.out.print("尝试像JVM堆缓冲区的方式引用内部数组.");
        }

        // 可以像如下方式读取为数组内容
        directByteBuffer.flip();
        dst = new byte[directByteBuffer.limit()];
        directByteBuffer.get(dst);
        // 转换为字符串打印
        System.out.println("dst => " + new String(dst, CharsetUtil.UTF_8));
        // 读取后，复原buffer的为读取前的状态，以便接下来的读取
        // directByteBuffer.rewind();
        directByteBuffer.flip();

        // 读取缓冲区的内容
        for (int i = 0; i < 10; i++) {
            System.out.print( directByteBuffer.get(i) + " ");
        }
    }
}
