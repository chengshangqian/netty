package com.fandou.learning.netty.action.chapter9;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.TooLongFrameException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试处理器/解码器异常
 */
class FrameChunkDecoderTest {
    @Test
    void testFrameChunkDecoded(){
        // 定义源数据，写入9个字节的数据
        ByteBuf source = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            source.writeByte(i);
        }

        // 派生一份源数据的拷贝，然后写入到channel中
        ByteBuf input = source.duplicate();

        // 定义一个3字节帧解码器并添加到channel的channelpipeline中
        EmbeddedChannel channel = new EmbeddedChannel(new FrameChunkDecoder(3));

        // 写入到channel中,如果使用readInbound方法能读取到数据(此过程会调用FrameChunkDecoder解码器)，返回true
        assertTrue(channel.writeInbound(input.readBytes(2)));

        try {
            // 写入4个字节，超出FrameChunkDecoder解码器可以解析的大小，正常情况下将抛出异常
            channel.writeInbound(input.readBytes(4));

            // 断言测试失败：如果没有抛出异常
            fail();
        } catch (TooLongFrameException ex){
            System.err.println("解码过程中抛出异常:" + ex.getMessage());
        }

        // 继续将剩下的数据3个字节写入到channel中,符合FrameChunkDecoder解码器的最大值,可以成功写入，返回true
        assertTrue(channel.writeInbound(input.readBytes(3)));
        // 将channel标记为完成状态
        assertTrue(channel.finish());

        // 模拟从channel中读取入站的数据帧
        ByteBuf read = (ByteBuf)channel.readInbound();

        // 前面两个字节正常写入,断言相等
        assertEquals(source.readSlice(2),read);
        read.release();

        // 继续读取数据帧，其值与源数据后3字节相同即跳过4个因写入异常失败的字节的后3个字节
        read = (ByteBuf)channel.readInbound();
        assertEquals(source.skipBytes(4).readSlice(3),read);

        // 释放资源
        read.release();
        source.release();
    }
}