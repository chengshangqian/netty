package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinedByteCharCodecTest {
    @Test
    void testCombinedByteCharCodec(){
        // 创建测试数据
        Character a = new Character('a');
        Character b = new Character('b');

        // 创建测试的channel
        EmbeddedChannel channel = new EmbeddedChannel(new CombinedByteCharCodec());

        // 入站数据写入channel
        ByteBuf buf = Unpooled.buffer();
        buf.writeChar(a);
        buf.writeChar(b);
        assertTrue(channel.writeInbound(buf));

        // 出站数据写入channel
        assertTrue(channel.writeOutbound(a));
        assertTrue(channel.writeOutbound(b));

        // 完成
        assertTrue(channel.finish());

        // 读取入站数据并断言：Byte中的字符被解码为Char
        assertEquals(a,channel.readInbound());
        assertEquals(b,channel.readInbound());

        // 读取出站数据并断言：Char被编码为Byte
        assertEquals(a,((ByteBuf)channel.readOutbound()).readChar());
        assertEquals(b,((ByteBuf)channel.readOutbound()).readChar());
    }
}