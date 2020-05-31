package com.fandou.learning.netty.core.chapter3;

import java.nio.ByteBuffer;

public class BufferWrap {
    public static void main(String[] args) {
        // 分配指定大小的缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);

        // 包装一个现有的数组
        byte[] array = new byte[10];
        ByteBuffer byteArrayBuffer = ByteBuffer.wrap(array);
    }
}
