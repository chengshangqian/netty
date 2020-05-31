package com.fandou.learning.netty.core.chapter3;

import org.junit.jupiter.api.Test;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedBufferTest {
    @Test
    void testMappedBuffer() throws Exception {
        // 定义内存映射（文件）位置的开始和大小
        int start = 0;
        int size = 1024;

        // 通过fileChannel和randomAccessFile读写文件
        RandomAccessFile randomAccessFile = new RandomAccessFile("D:/tmp/logs/springbootvue/mylog.log","rw");
        FileChannel fileChannel = randomAccessFile.getChannel();

        // 内存映射：内存与文件内容位置的映射
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,start,size);

        // 文件开头写入字母a
        mappedByteBuffer.put(0,(byte)97);
        // 文件末尾（1023）位置写入字母z
        mappedByteBuffer.put(1023,(byte)122);

        // 关闭
        randomAccessFile.close();
    }
}
